#!/bin/sh
module load oracle-jdk/1.8.0_72

./activator stopProd

kill -TERM $(cat RUNNING_PID)
kill -9 $(cat RUNNING_PID)
rm RUNNING_PID

