#!/bin/sh

LOG=prod-logback.xml

#nohup ./betterplay &
nohup ./betterplay -Dhttp.port=9055  -Dlogger.resource=$LOG -Dconfig.resource=production.conf &
#nohup ./betterplay -Dhttp.port=9000  -Dlogger.resource=$LOG -Dconfig.resource=production.conf &



