#!/bin/sh

conda activate py38

#--headless

locust -f test_betting.py  


#lsof -i -P | grep 8089 | grep python | awk '{system("kill " $2); system("kill -9 " $2)}' 

