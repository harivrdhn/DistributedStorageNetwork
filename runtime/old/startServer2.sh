#!/bin/bash
#
# This script is used to start the server: two
#

export POKE_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "** starting server from ${POKE_HOME} **"

cd ${POKE_HOME}

JAVA_MAIN='poke.server.Server'
JAVA_ARGS="${POKE_HOME}/server2.conf"
echo "** config: ${JAVA_ARGS} **"

# see http://java.sun.com/performance/reference/whitepapers/tuning.html
JAVA_TUNE='-Xms500m -Xmx1000m'


java ${JAVA_TUNE} -cp .:${POKE_HOME}/../lib/'*':${POKE_HOME}/../classes ${JAVA_MAIN} ${JAVA_ARGS} 
