#!/bin/bash
# shellcheck disable=SC2115,SC2155,SC2015,SC2034
#
#################################################################
# shellcheck disable=SC2086
PROGNAME="$(basename ${0})"
SELMODE="$(awk -F= '/^SELINUX=/{print $2}' /etc/selinux/config)"
# Ensure we'v got our CFn envs (in case invoking via other than CFn)
while read -r ENV
do
  # shellcheck disable=SC2163
  export "${ENV}"
done < /etc/cfn/Confluence.envs
BINSTALL=/root/atlassian-confluence-installer_x64.bin
RESPFILE=/root/response.varfile
SHARESRVR="${CONFLUENCE_SHARE_SERVER:-UNDEF}"
PROXYFQDN="${CONFLUENCE_PROXY_FQDN:-UNDEF}"
SERVERXML="/opt/atlassian/confluence/conf/server.xml"

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
## Create persistent content-dirs in shared fileservice
function MkPersistDir {
   local NEWDIR="${1}"

   if [[ -d ${NEWDIR} ]]
   then
      err_exit "Found ${NEWDIR} where none should have existed"
   else
      install -d -m 755 /mnt/${NEWDIR} || \
        err_exit "Failed to create ${SHARESRVR}:/${NEWDIR}"
   fi
}

##
## Mount persistent content-dirs
function MtPersistDir {
   local SRCDIR="${1}"
   # shellcheck disable=SC2155
   local DSTDIR="$(echo ${SRCDIR} | sed -e 's#_#/#' -e 's#^#/#')"

   # Verify source exists (fix as necessary)
   if [[ ! -d /mnt/${SRCDIR} ]]
   then
      err_exit "Aborting: ${SRCDIR} does not exist."
   fi

   # Verify destination exists (fix as necessary)
   if [[ ! -d ${DSTDIR} ]]
   then
      install -d -m 755 "${DSTDIR}" ||
        err_exit "Could not create ${DSTDIR}"
   fi

   printf "Attempting to mount %s... " "${DSTDIR}"
   mount "${SHARESRVR}":/"${SRCDIR}" "${DSTDIR}" && echo "Success" ||
     err_exit "Failed to mount ${DSTDIR}"
}

##
## Nuke the dummy data
function CleanDummy {
   local DUMMYDIR="$(echo ${1} | sed -e 's#_#/#' -e 's#^#/#')"

   printf "Attempting to clean %s... " "${DUMMYDIR}"
   rm -rf "${DUMMYDIR}"/* 2> /dev/null && \
     echo "Success" || \
     err_exit "Failed to remove all files from ${DUMMYDIR}"
}


##
## Main script logic
setenforce 0


# Make ready for unattended install
cat > "${RESPFILE}" << EOF
executeLauncherAction\$Boolean=true
app.install.service\$Boolean=true
sys.confirmedUpdateInstallationString=false
launch.application\$Boolean=true
existingInstallationDir=/opt/Confluence
sys.languageId=en
sys.installationDir=/opt/atlassian/confluence
EOF

# Prep storage for install
mount "${SHARESRVR}":/ /mnt 2> /dev/null || \
  err_exit 'Cannot mount shared filesystems server'

if [ "$(find /mnt -name server.xml)" = "" ]
then
   echo "This is a fresh install"

   bash "${BINSTALL}" -q -varfile "${RESPFILE}" || \
     err_exit 'Installer did not run to clean completion'

   while [[ $( netstat -46ln | grep -q :8000 )$? -ne 0 ]]
   do
      echo "Waiting for Confluence to finish initial start-up..."
      sleep 10
   done

   printf 'Attempting to stop Confluence for reconfiguration... '
   service confluence stop && echo 'Success' || \
     err_exit 'Failed to stop Confluence'

   echo "Relocating to persistent storage..."
   mv /opt/atlassian /mnt/opt_atlassian || \
     err_exit 'Failed to re-home /opt/atlassian to persistent storage'
   mv /var/atlassian /mnt/var_atlassian || \
     err_exit 'Failed to re-home /var/atlassian to persistent storage'

   # Create and mount persistent-content dirs
   for DIR in opt_atlassian var_atlassian
   do
      MtPersistDir "${DIR}"
   done

   # Make sure Confluence doesn't complain about being behind an ELB
   if [[ ${PROXY} = UNDEF ]]
   then
      echo "No proxy-host passed to install-wrapper."
   elif [[ -e ${SERVERXML} ]]
   then
      printf 'Attempting to add proxy-host to server.xml... '
      # shellcheck disable=SC1004
      sed -i '/Connector port="8090"/a \
                proxyName="'${PROXYFQDN}'" proxyPort="443" scheme="https"' "${SERVERXML}" &&
        echo 'Success' || \
        err_exit 'Failed to add proxy-host to server.xml'
   else
      err_exit "Unable to find ${SERVERXML} to massage"
   fi

   printf 'Attempting post-reconfiguration restart of Confluence... '
   service confluence start && echo 'Success' || \
     err_exit 'Failed to restart Confluence'

else
   echo "This is a rebuild"
   bash "${BINSTALL}" -q -varfile "${RESPFILE}" || \
     err_exit 'Installer did not run to clean completion'

   while [[ $( netstat -46ln | grep -q :8000 )$? -ne 0 ]]
   do
      echo "Waiting for Confluence to finish initial start-up..."
      sleep 10
   done

   service confluence stop || \
     err_exit 'Failed to stop disposable Confluence install'
   for DIR in opt_atlassian var_atlassian
   do
      CleanDummy "${DIR}"
      MtPersistDir "${DIR}"
   done
   service confluence start || \
     err_exit 'Failed to start re-deployed Confluence application'
fi

umount "${SHARESRVR}":/ 2> /dev/null || \
  err_exit 'Cannot umount shared filesystems-root'

grep "${SHARESRVR}" /proc/mounts >> /etc/fstab || \
  err_exit 'Failed to update /etc/fstab'

# Enable Confluence systemd service (as necessary)
if [[ $(systemctl is-enabled confluence) == disabled ]]
then
    printf 'Enabling Confluence systemd service... '
    systemctl --quiet enable confluence && echo "Success" || \
      err_exit 'Failed to enable Confluence systemd service'
fi

setenforce ${SELMODE}
