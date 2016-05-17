#!/bin/sh
module load oracle-jdk/1.8.0_72

./activator stopProd

kill -TERM $(cat target/universal/stage/RUNNING_PID)
kill -9 $(cat target/universal/stage/RUNNING_PID)
rm target/universal/stage/RUNNING_PID

