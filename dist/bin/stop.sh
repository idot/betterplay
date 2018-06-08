#!/bin/sh


kill -TERM $(cat ../../RUNNING_PID)
kill -9 $(cat ../../RUNNING_PID)
rm ../../RUNNING_PID

