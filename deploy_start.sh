#!/bin/sh

#to be run on the server 

VERSION=betterplay-0.9-SNAPSHOT

kill -TERM $(cat ${VERSION}/RUNNING_PID)
sleep 3
kill -9 $(cat ${VERSION}/RUNNING_PID)
sleep 3
rm -f ${VERSION}/RUNNING_PID
sleep 3
rm -rf $VERSION
unzip ${VERSION}.zip
chmod u+x ${VERSION}/bin/*.sh ${VERSION}/bin/betterplay
cd ${VERSION}/bin
./start.sh
tail -f nohup.out


