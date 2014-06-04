#!/bin/sh
kill -TERM $(cat target/universal/stage/RUNNING_PID)
kill -9 $(cat target/universal/stage/RUNNING_PID)
rm target/universal/stage/RUNNING_PID

