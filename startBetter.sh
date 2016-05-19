#!/bin/sh



module unload java
module load oracle-jdk/1.8.0_72
export JAVA_HOME=/groups/vbcf-ngs/bin/lib/java/jdk1.8.0_92


BETTER="betterplay-0.8-SNAPSHOT"
export BETTER

kill -TERM $(cat ${BETTER}/RUNNING_PID)
sleep 1
kill -9 $(cat ${BETTER}/RUNNING_PID)
sleep 2

rm ${BETTER}/RUNNING_PID
rm ${BETTER}/nohup.out
rm ${BETTER}/logs/*

rm -rf ${BETTER}
tar -xvzf ${BETTER}.tgz
cd ${BETTER}

LOG=prod-logback.xml
PROD=/groups/vbcf-ngs/programs/betterplay-0.8-SNAPSHOT/conf/production.conf

nohup bin/betterplay  -Dconfig.file=$PROD -Dlogger.resource=$LOG -Dhttp.port=9055 &
echo "SET THE MAIL PASSWORD!!!!"

tail -f nohup.out

