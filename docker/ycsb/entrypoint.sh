#!/bin/bash

mkdir /results && mkdir -p $EXP_OUTDIR && mount --bind $EXP_OUTDIR /results
mkdir -p /home/anatole/jdk8u/build/linux-x86_64-normal-server-release/jdk && mount --bind $JAVA_HOME /home/anatole/jdk8u/build/linux-x86_64-normal-server-release/jdk
rm -f ${EXP_OUTDIR}/*

cd /ycsb/exp/
./${EXP_NAME:-runall}.sh
./log_to_data.sh ${EXP_NAME//_/.}

for exp_plot in /ycsb/plot/${EXP_NAME}*.gp ; do
    gnuplot $exp_plot
done
