#!/bin/bash

SCRIPTNAME=`basename $0`

require_env_var() {
    varname=$1
    [ -z ${!varname} ] \
      && echo "$SCRIPTNAME - ERROR - Required environment variable $varname is empty" \
      && exit 1
}

require_env_var "JAVA_HOME"
require_env_var "EXP_OUTDIR"
#require_env_var "EXP_NAME"

#Dirty fix for hardcoded JAVA_HOME export in latter exp scripts
hard_jdkpath="/home/anatole/jdk8u/build/linux-x86_64-normal-server-release/jdk"
mkdir -p $hard_jdkpath && mount --bind $JAVA_HOME $hard_jdkpath

cd /ycsb/exp/

case $EXP_NORUN in
  y|yes|true)
    echo "EXP_NORUN set to $EXP_NORUN, skipping ${EXP_NAME:-every} run"
    ;;
  *)
    #hacky way to clear either one are all old exp records in one container start
    rm -rf ${EXP_OUTDIR}/${EXP_NAME//_/.}*.ref/*
    ./${EXP_NAME:-runall}.sh
    ;;
esac

./log_to_data.sh ${EXP_NAME//_/.}

for exp_plot in /ycsb/plot/${EXP_NAME}*.gp ; do
    gnuplot -e "outdir='${EXP_OUTDIR}'" $exp_plot
done
