#!/bin/sh

module load oracle-java-

activator clean stage
#./target/universal/stage/bin/betterplay -Dconfig.file=./conf/production.conf -Dhttp.port=9055
./target/universal/stage/bin/betterplay -Dhttp.port=9055


