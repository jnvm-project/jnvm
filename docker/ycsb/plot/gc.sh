#!/usr/bin/env bash

if [ $# -ne 1 ]; then
    echo "usage ycsb_log_file"
    exit -1
fi

log=$1
total=$(cat ${log} | grep "\[UPDATE\]" | grep Operations | awk '{print $3}')
#cat ${log} | grep "\[UPDATE\]" | awk -F', ' '{print $2" "$3}' | egrep "^[0-9]+ " | awk -v total="${total}" '{sum+=$2; print sum/total" "$1}' | awk '{print int($2/1000)" "$1}' | uniq
cat ${log} | egrep "\[UPDATE\], [0-9]+, [0-9]+" | awk -v total="${total}" '{lat=$2; sum+=$3; printf("%d %.8f\n", int(lat/1000), sum/total)}'
