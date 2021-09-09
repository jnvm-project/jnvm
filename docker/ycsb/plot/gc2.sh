#!/bin/bash

#mems="numa interleaved default preferred"
#oops="expended default compressed"
n_run=6
mems="interleaved"
oops="default"
entrycounts="15000000 1500000 150000"

[ $# -ge 1 ] && entrycounts=$1

echo "ratio m/p compute-avg compute-min compute-max gc-avg gc-min gc-max heap"
for i in $entrycounts ; do
for oop in $oops ; do
for mem in $mems ; do
  run_avg=0;run_min=0;run_max=0
  run_gc_avg=0;run_gc_min=0;run_gc_max=0
  for k in `seq 1 $n_run` ; do
    log=${EXP_OUTDIR}/exp00.heapsize.ref/log/infinispan.run.workloadf.true.15000000.${i}.10.zipfian.10.false.true.$mem.$oop.r${k}.log
    [ -f ${log} ] || continue

    ratio=$(echo "scale=2; $(cat ${log} | grep -i xmx | awk '{print $2}' | sed s/-Xmx//g | sed s/g//g)/80"| bc)
    heap=$(cat ${log} | grep -i xmx | awk '{print $2}' | sed s/-Xmx//g | sed s/g/GB/g)
    gc=$(cat ${log} | grep "TOTAL_GC_TIME]" | awk '{print $3*10^-3}')
    run_gc=$(cat ${log} | grep "MAIN_GCs_Time" | grep Average | awk '{print $3*10^-3}')
    run=$(echo "scale=2;$(cat ${log} | grep "\[TRANSACTION\]" | grep Average | awk '{print $3*10^-9}')-${run_gc}"|bc)
    load_gc=$(echo "$gc - $run_gc" | bc)
    load=$(echo "$(cat ${log} | grep "INIT" | grep Average | awk '{print $3*10^-9}')-${load_gc}"|bc)
    ratio=$(echo "scale=0; 100*"${i}/"15000000"| bc)
    m=$(echo $mem | head -c 1)
    p=$(echo $oop | head -c 1)

    run_avg=$(echo "$run_avg + $run" | bc)
    run_gc_avg=$(echo "$run_gc_avg + $run_gc" | bc)
    [ `echo "$run_min <= 0" | bc -l` -eq 1 ] && run_min=$run
    [ `echo "$run_gc_min <= 0" | bc -l` -eq 1 ] && run_gc_min=$run_gc
    [ `echo "$run > $run_max" | bc -l` -eq 1 ] && run_max=$run
    [ `echo "$run < $run_min" | bc -l` -eq 1 ] && run_min=$run
    [ `echo "$run_gc > $run_gc_max" | bc -l` -eq 1 ] && run_gc_max=$run_gc
    [ `echo "$run_gc < $run_gc_min" | bc -l` -eq 1 ] && run_gc_min=$run_gc
  done
  [ `echo "$run_avg > 0" | bc -l` -eq 0 ] && continue
  run_avg=$(echo "$run_avg / $n_run"  | bc -l)
  run_gc_avg=$(echo "$run_gc_avg / $n_run" | bc -l)

  run_avg=$(echo "$run_avg / 60" | bc -l)
  run_min=$(echo "$run_min / 60" | bc -l)
  run_max=$(echo "$run_max / 60" | bc -l)
  run_gc_avg=$(echo "$run_gc_avg / 60" | bc -l)
  run_gc_min=$(echo "$run_gc_min / 60" | bc -l)
  run_gc_max=$(echo "$run_gc_max / 60" | bc -l)

  echo ${ratio}"\\\%" "${m}\/${p}" ${run_avg} ${run_min} ${run_max} ${run_gc_avg} ${run_gc_min} ${run_gc_max} ${heap}
done
done
done
