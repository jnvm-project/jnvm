set terminal png
set output outdir.'/exp4.objsize.ref/ycsb_objsize.png'

load "/ycsb/plot/styles.inc"

set title ""
set auto x
set style data histogram
set style fill solid border -1
set boxwidth 0.9
set border 1

set xtics offset 0,-1 rotate by -45 left nomirror
set ytics nomirror

set key below right samplen 2 width 15 spacing 2 vertical maxrows 3
set key tmargin

set lmargin 10                                                               
set rmargin 5                                                               
set tmargin 5                                                            
set bmargin 10

set ylabel "Latency ($\\mu$s)" offset -2,0
set xlabel "Record size" offset 0,-4

set logscale y
set yrange [0:10000]

unset key
unset ytics
unset ylabel

plot \
"< cat ${EXP_OUTDIR}/exp4.objsize.ref/data/exp4.objsize.dat | grep read | grep \"nvm\" | awk -F, '{printf \"%dB %2.2f\\n\", (10*$3), ($10/1000)}' | sed s/000000B/MB/g | sed s/000B/KB/g" using ($2):xtic(1) title "read (\\SYS)" ls 1, \
"< cat ${EXP_OUTDIR}/exp4.objsize.ref/data/exp4.objsize.dat | grep read | grep \"infinispan,\" | awk -F, '{printf \"%dB %2.2f\\n\", (10*$3), ($10/1000)}' | sed s/000000B/MB/g  | sed s/000B/KB/g" using ($2):xtic(1) title "read (DAX)" ls 2, \
"< cat ${EXP_OUTDIR}/exp4.objsize.ref/data/exp4.objsize.dat | grep update | grep \"nvm\" | awk -F, '{printf \"%dB %2.2f\\n\", (10*$3), ($10/1000)}' | sed s/000000B/MB/g | sed s/000B/KB/g" using ($2):xtic(1) title "update (\\SYS)" ls 1 fs pattern 1, \
"< cat ${EXP_OUTDIR}/exp4.objsize.ref/data/exp4.objsize.dat | grep update | grep \"infinispan,\" | awk -F, '{printf \"%dB %2.2f\\n\", (10*$3), ($10/1000)}' | sed s/000000B/MB/g  | sed s/000B/KB/g" using ($2):xtic(1) title "update (DAX)" ls 2 fs pattern 1

