#!/bin/bash

docker pull gingerbreadz/transactions:latest

rm -f /${EXP_OUTDIR}/*

/tpcb/src/test/bin/fault.sh

gnuplot -e "outdir='${EXP_OUTDIR}'" /tpcb/plot.gp
