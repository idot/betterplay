#!/bin/sh

./activator clean stage universal:packageZipTarball
scp target/universal/betterplay-0.8-SNAPSHOT.tgz solexa@gecko:/groups/vbcf-ngs/programs
scp startBetter.sh solexa@gecko:/groups/vbcf-ngs/programs
ssh solexa@gecko /groups/vbcf-ngs/programs/startBetter.sh
#must clean for gulp
./activator clean compile 

