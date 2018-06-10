#!/bin/sh

#deploys the dist to the server. does not start it. 
#log in on server and execute deploy_server.sh

SERVER=better:/home/centos/better

sbt dist
if [ $? -eq 0 ]; then
    scp target/universal/betterplay-0.9-SNAPSHOT.zip $SERVER
    scp deploy_start.sh $SERVER
else
    echo "problem with sbt dist"
fi

