#!/bin/bash

while read -r pid
do
    kill $pid
done < logs/pids
