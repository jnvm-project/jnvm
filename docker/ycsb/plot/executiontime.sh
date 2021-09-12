#!/bin/bash

for w in a b c d f;
do
    echo -n $w\" \" | tr a-z A-Z;
    for s in infinispan, infinispan-jnvm, infinispan-pcj, infinispan-jpfa;
    do
       cat ${EXP_OUTDIR}/exp0.exectime.ref/data/exp0.exectime.dat | grep workload${w} | grep transaction | grep ${s} | grep "run," | awk -F, '{printf (1e6)*$5/$NF" "}';
    done | awk '{for (i=1; i<=NF; i++) printf $i" ";print ""}';
done | sed s/\"//g
