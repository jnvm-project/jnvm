#!/usr/bin/env bash

if [ $# -ne 1 ]; then
    echo "usage [ <ycsb_log_file> | <cache ratio> ]"
    exit -1
fi

recordcount="15000000"

#external parameter overrides
if [ $EXP_PRESET == "tiny" ] ; then
recordcount="10000"
fi

case $1 in
  1)
    log="${EXP_OUTDIR}/exp00.heapsize.ref/log/infinispan.run.workloadf.true.${recordcount}.$(( recordcount / 100 )).10.100.zipfian.10.false.true.default.default.r1.log"
    ;;
  10)
    log="${EXP_OUTDIR}/exp00.heapsize.ref/log/infinispan.run.workloadf.true.${recordcount}.$(( recordcount / 10 )).10.100.zipfian.10.false.true.default.default.r1.log"
    ;;
  100)
    log="${EXP_OUTDIR}/exp00.heapsize.ref/log/infinispan.run.workloadf.true.${recordcount}.$(( recordcount )).10.100.zipfian.10.false.true.default.default.r1.log"
    ;;
  *)
    log=$1
    ;;
esac

total=$(cat ${log} | grep "\[UPDATE\]" | grep Operations | awk '{print $3}')
#cat ${log} | grep "\[UPDATE\]" | awk -F', ' '{print $2" "$3}' | egrep "^[0-9]+ " | awk -v total="${total}" '{sum+=$2; print sum/total" "$1}' | awk '{print int($2/1000)" "$1}' | uniq
cat ${log} | egrep "\[UPDATE\], [0-9]+, [0-9]+" | awk -v total="${total}" '{lat=$2; sum+=$3; printf("%d %.8f\n", int(lat/1000), sum/total)}'
