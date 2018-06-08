#!/bin/sh


kill -TERM $(cat ../RUNNING_PID)
sleep 3
kill -9 $(cat ../RUNNING_PID)
sleep 3
rm ../RUNNING_PID


