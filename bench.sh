#!/bin/bash

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SCRIPT_DIR=$(realpath "${SCRIPT_DIR}")

#DEFAULTS
#export PMEM_MOUNT="/pmem0"
#export TMPFS_MOUNT="/dev/shm"
#export NULLFS_MOUNT="/blackhole"

#export NUMA_NODE=0
#export JHEAP_SIZE="20g"
export RESULT_DIR=$SCRIPT_DIR/results


check_env() {
  for env_var in PMEM_MOUNT TMPFS_MOUNT NULLFS_MOUNT NUMA_NODE JHEAP_SIZE RESULT_DIR; do
    echo "$env_var: ${!env_var}"
  done
}

case $1 in
  check-env) check_env && exit 0;;
  pull-all)
    docker pull gingerbreadz/ycsb:latest
    docker pull gingerbreadz/transactions:latest
    docker pull gingerbreadz/tpcb_client:latest
    docker pull yohanpipereau/go-pmem:latest
    exit 0
    ;;
  ycsb*)
    RESULT_DIR_IN="/ycsb/exp/out"
    DOCKER_ARGS="--mount type=bind,source=${PMEM_MOUNT},destination=/pmem0 \
                 --mount type=bind,source=${TMPFS_MOUNT},destination=/dev/shm \
                 --mount type=bind,source=${NULLFS_MOUNT},destination=/blackhole \
                 -e NUMA_NODE=${NUMA_NODE} \
                 -e EXP_OUTDIR=${RESULT_DIR_IN} \
                 -e JHEAP_SIZE=${JHEAP_SIZE}"
    DOCKER_IMAGE="gingerbreadz/ycsb:latest"
    ;;&
  ycsb_all)
    EXP_NAME=""
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_throughput)
    EXP_NAME="exp0_exectime"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_marshalling)
    EXP_NAME="exp7_marshalling"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_cacheratio)
    EXP_NAME="exp1_cachesize"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_recordcount)
    EXP_NAME="exp2_keycount"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_fieldcount)
    EXP_NAME="exp6_fieldcount"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_recordsize)
    EXP_NAME="exp4_objsize"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_concurrent)
    EXP_NAME="exp5_concurrent"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_pdt)
    EXP_NAME="exp8_pdt"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  ycsb_heapsize)
    EXP_NAME="exp00_heapsize"
    DOCKER_ARGS="$DOCKER_ARGS -e EXP_NAME=$EXP_NAME"
    ;;
  tpcb)
    RESULT_DIR_IN="/results"
    DOCKER_ARGS="--mount type=bind,source=/tmp/bank,destination=/tmp/bank \
                 -v /var/run/docker.sock:/var/run/docker.sock --net host \
                 -e PMEM_MOUNT=${PMEM_MOUNT} \
                 -e NUMA_NODE=${NUMA_NODE} \
                 -e EXP_OUTDIR=${RESULT_DIR_IN} \
                 -e JHEAP_SIZE=${JHEAP_SIZE}"
    DOCKER_IMAGE="gingerbreadz/tpcb_client:latest"
    ;;
  go-pmem)
    RESULT_DIR_IN="/results"
    DOCKER_ARGS="--mount type=bind,source=${PMEM_MOUNT},destination=/pmem0 \
                 -e MIN_ORDER=17 -e MAX_ORDER=24 -e NUMA_NODE=${NUMA_NODE}"
    DOCKER_IMAGE="yohanpipereau/go-pmem"
    ;;
  *)
    echo "Unrecognized input arg" && exit 1
    ;;
esac

mkdir -p $RESULT_DIR

docker run --rm -d --privileged \
    -v $RESULT_DIR:$RESULT_DIR_IN \
    $DOCKER_ARGS \
    $DOCKER_IMAGE
