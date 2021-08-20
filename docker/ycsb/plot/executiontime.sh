#!/bin/bash

for w in a b c d f;
do
    echo -n $w\" \" | tr a-z A-Z;
    for s in infinispan, infinispan-jnvm, infinispan-pcj, infinispan-jpfa;
    do
	cat /results/data/exp0.exectime.dat | grep workload${w} | grep transaction | grep ${s} | grep "run," | awk -F, '{printf $11" "}';
    done | awk '{for (i=1; i<=NF; i++) printf 10*1e12/$i" ";print ""}';
done | sed s/\"//g
