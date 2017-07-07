#!/bin/bash
# shellcheck disable=SC2015
#
# Script to prepare the OS for installation of the Confluence
# web application.
#
#################################################################
# shellcheck disable=SC2086
PROGNAME="$(basename ${0})"
SELMODE="$(awk -F= '/^SELINUX=/{print $2}' /etc/selinux/config)"
SHARESRVR="${CONFLUENCE_SHARE_SERVER:-UNDEF}"
BINURL="${CONFLUENCE_BIN_URL:-UNDEF}"

##
## Set up an error logging and exit-state
function err_exit {
   local ERRSTR="${1}"
   local SCRIPTEXIT=${2:-1}

   # Our output channels
   echo "${ERRSTR}" > /dev/stderr
   logger -t "${PROGNAME}" -p kern.crit "${ERRSTR}"

   # Need our exit to be an integer
   if [[ ${SCRIPTEXIT} =~ ^[0-9]+$ ]]
   then
      exit "${SCRIPTEXIT}"
   else
      exit 1
   fi
}

##
## Enable services needed by NFS-client
function NfsClientSetup {
   local NFSSVCS=(
               rpcbind
               nfs-lock
               nfs-idmap
            )
   for SVC in "${NFSSVCS[@]}"
   do
      printf "Enabling %s... " "${SVC}"
      systemctl enable "${SVC}" && echo "Success" || \
        err_exit "Failed to enable ${SVC}"

      printf "Starting %s... " "${SVC}"
      systemctl start "${SVC}" && echo "Success" || \
        err_exit "Failed to start ${SVC}"
   done
}


###########################
## Main program contents ##
###########################

##
## Create firewalld service-definition for Confluence
printf "Creating service-definition template for Confluence... "
cat > /tmp/confluence.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<service>
   <short>Confluence</short>
   <description>Confluence documentation collaboration service</description>
   <port protocol="tcp" port="8000"/>
   <port protocol="tcp" port="8090"/>
</service>
EOF

# shellcheck disable=SC2181
if [[ $? -eq 0 ]]
then
   echo "Success"
else
   err_exit 'Failed to create the Confluence firewalld template'
fi

##
## Configure firewall
if [[ ${SELMODE} = enforcing ]]
then
   printf "Dialing back SEL mode... "
   setenforce 0 || err_exit 'Failed to loosen SEL enforcement-mode'
fi

printf "Reading service-definition template for Confluence... "
firewall-offline-cmd --new-service-from-file=/tmp/confluence.xml && \
  echo "Success" || \
  err_exit 'Failed reading firewalld service-definition for Confluence'

printf "Restarting firewalld... "
systemctl restart firewalld && \
  echo "Success" || \
  err_exit 'Failed restarting firewalld'

echo "Getting firewalld info for Confluence"
(
  firewall-cmd --permanent --service=confluence --get-short
  firewall-cmd --permanent --service=confluence --get-description
  firewall-cmd --info-service=confluence
  firewall-cmd --permanent --add-service=confluence
  iptables -n -L IN_public_allow
) | sed 's /^/  '

setenforce ${SELMODE} || err_exit "Failed setting SEL mode to ${SELMODE}"

##
## Enabling services
NfsClientSetup

# Supplementary mounts...
if [[ ! -d /var/atlassian/cluster ]]
then
   printf "Creating /var/atlassian/cluster... "
   install -d -m 0700 /var/atlassian/cluster && \
     echo "Success" || \
     err_exit 'Failed to create /var/atlassian/cluster'
fi

printf "Mounting cluster shared directory... "
mount "${SHARESRVR}":/atlassian/cluster /var/atlassian/cluster && \
  echo "Success" || \
  err_exit 'Failed to mount cluster directory'

# Stage the Confluence binary-installer
printf "Fetching Confluence binary-installer... "
curl -skL "${BINURL}" -o /root/atlassian-confluence-installer_x64.bin && \
   echo "Success" || \
   err_exit 'Failed to download the binary-installer'

