#!/bin/bash

case $EXP_NORUN in
  y|yes|true)
    echo "EXP_NORUN set to $EXP_NORUN, skipping $EXP_NAME run"
    ;;
  *)
    docker pull gingerbreadz/transactions:latest
    rm -rf ${EXP_OUTDIR}/*
    /tpcb/src/test/bin/fault.sh
    ;;
esac

gnuplot -e "outdir='${EXP_OUTDIR}'" /tpcb/plot.gp
