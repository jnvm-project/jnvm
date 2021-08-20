#!/bin/bash

log=$1

cat $1 | sed 's;^\[\([0-9]*\):\([0-9]*\)\];\1\.\2;' | awk -v window=20 '(NR==5){c=$1;s=$1} ($2~ "[0-9]") {i+=1; sumv=0; sumt=0; dt=$1-c; t=$1-s; v=$2; for(j=0;j<window-1;j++){tabv[j]=tabv[j+1];tabt[j]=tabt[j+1];sumt+=tabt[j];sumv+=tabv[j]}; tabt[window-1]=t; tabv[window-1]=v; sumv+=v; sumt+=dt; Dt=tabt[window-1]-tabt[0]; if(i>10) print t" "1e-3*sumv/Dt; c=$1}'
