#!/bin/sh

#deploys the dist to the server. does not start it. 
#log in on server and execute deploy_server.sh

SERVER=${EC2_USER}@${EC2_INSTANCE}

sbt dist
if [ $? -eq 0 ]; then
    scp -v -i ${EC2_PEM} target/universal/betterplay-0.9-SNAPSHOT.zip ${SERVER}:~
    scp -v -i ${EC2_PEM} deploy_start.sh $SERVER:~
else
    echo "problem with sbt dist"
fi

