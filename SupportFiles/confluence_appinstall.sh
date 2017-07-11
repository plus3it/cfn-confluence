#!/bin/bash
# shellcheck disable=SC2115,SC2155,SC2015,SC2034
#
#################################################################
# shellcheck disable=SC2086
PROGNAME="$(basename ${0})"
SELMODE="$(awk -F= '/^SELINUX=/{print $2}' /etc/selinux/config)"
BINSTALL=/root/atlassian-confluence-installer_x64.bin
RESPFILE=/root/response.varfile
SHARESRVR="${CONFLUENCE_SHARE_SERVER:-UNDEF}"

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

   # Create and mount persistent-content dirs
   for DIR in opt_atlassian var_atlassian
   do
      MkPersistDir "${DIR}" 
      MtPersistDir "${DIR}"
   done

   bash "${BINSTALL}" -q -varfile "${RESPFILE}" || \
     err_exit 'Installer did not run to clean completion'
else
   echo "This is a rebuild"
   bash "${BINSTALL}" -q -varfile "${RESPFILE}" || \
     err_exit 'Installer did not run to clean completion'
   service confluence stop
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
