#!/bin/sh

activator clean stage
./target/universal/stage/bin/betterplay -Dconfig.file=conf/production.conf -Dhttp.port=9050



