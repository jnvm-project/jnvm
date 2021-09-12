#!/bin/bash

dts="blackhole blackhole-offheap vhmap rhmap vtmap rtmap vslmap rslmap"

[ $# -eq 2 ] && dts="$1 $2"

recordcount="1000000"
file="${EXP_OUTDIR}/exp8.pdt.ref/data/exp8.pdt.dat"

#external parameter overrides
if [ $EXP_PRESET == "tiny" ] ; then
recordcount="10000"
fi

echo "datatype read update gc execution"
for dt in $dts ; do
  read=`cat $file | awk -F',' -v d="$dt" -v op="read" -v r="r1" '($2=='${recordcount}') && ($7 == "jnvm-"d) && ($9 ~ "true") && ($19 ~ op) && ($18 ~ r) { if($19 ~ op"_lat") lat[$18]=$NF; else if($19 ~ op"_ops") n_op[$18]=$NF;} END{ lat_avg=0; for (i in lat) { lat_avg+=lat[i]*n_op[i]; } print lat_avg/length(lat)/1e9; }'`
  update=`cat $file | awk -F',' -v d="$dt" -v op="update" -v r="r1" '($2=='${recordcount}') && ($7 == "jnvm-"d) && ($9 ~ "true") && ($19 ~ op) && ($18 ~ r) { if($19 ~ op"_lat") lat[$18]=$NF; else if($19 ~ op"_ops") n_op[$18]=$NF;} END{ lat_avg=0; for (i in lat) { lat_avg+=lat[i]*n_op[i]; } print lat_avg/length(lat)/1e9; }'`
  gc=`cat $file | awk -F',' -v d="$dt" -v op="main_gc_time_ms" -v r="r1" '($2=='${recordcount}') && ($7 == "jnvm-"d) && ($9 ~ "true") && ($19 ~ op) && ($18 ~ r) { sum+=$NF; n+=1 } END{ print sum/n/1e3 }'`
  transaction=`cat $file | awk -F',' -v d="$dt" -v op="transaction" -v r="r1" '($2=='${recordcount}') && ($7 == "jnvm-"d) && ($9 ~ "true") && ($19 ~ op) && ($18 ~ r) { sum+=$NF; n+=1 } END{ print sum/n/1e9 }'`
  #echo $dt $read $update $gc $transaction
  echo $dt $read $update $gc `echo "$transaction-$read-$update-$gc" | bc`
done
