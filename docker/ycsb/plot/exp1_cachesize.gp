set terminal png
set output outdir.'/exp1.cachesize.ref/ycsb_cachesize.png'

load "/ycsb/plot/styles.inc"

set title ""
set auto x
set style data histogram
set style fill solid border -1
set boxwidth 0.9
set border 3

set xtics offset 0,-1 rotate by -45 left nomirror
set ytics nomirror

set key above right samplen 2 width 15 spacing 2 vertical maxrows 3
set key tmargin

set lmargin 10                                                               
set rmargin 5                                                               
set tmargin 5                                                            
set bmargin 10

set ylabel "Latency ($\\mu$s)" offset -2,0
set xlabel "Cache ratio (\\%)" offset 0,-4.5

set logscale y
set yrange [0:10000]

unset key

plot \
"< cat ${EXP_OUTDIR}/exp1.cacheratio.ref/data/exp1.cachesize.dat | egrep -v \"^(300000|900000|1500000|2100000),\" | grep read | grep nvm | awk -F \",\" '{printf \"%3.0f %3.3f\\n\", (($1/3000000)*100), ($10/1000)}' | sort -n " using ($2):xtic(1) title "\\SYS" ls 1, \
"< cat ${EXP_OUTDIR}/exp1.cacheratio.ref/data/exp1.cachesize.dat | egrep -v \"^(300000|900000|1500000|2100000),\" | grep read | grep \"infinispan,\" | awk -F \",\" '{printf \"%3.0f %3.3f\\n\", (($1/3000000)*100), ($10/1000)}' | sort -n" using ($2):xtic(1) title "DAX" ls 2, \
"< cat ${EXP_OUTDIR}/exp1.cacheratio.ref/data/exp1.cachesize.dat | egrep -v \"^(300000|900000|1500000|2100000),\" | grep update | grep nvm | awk -F \",\" '{printf \"%3.0f %3.3f\\n\", (($1/3000000)*100), ($10/1000)}' | sort -n " using ($2):xtic(1) title "\\SYS" ls 1 fs pattern 1, \
"< cat ${EXP_OUTDIR}/exp1.cacheratio.ref/data/exp1.cachesize.dat | egrep -v \"^(300000|900000|1500000|2100000),\" | grep update | grep \"infinispan,\" | awk -F \",\" '{printf \"%3.0f %3.3f\\n\", (($1/3000000)*100), ($10/1000)}' | sort -n" using ($2):xtic(1) title "DAX" ls 2 fs pattern 1
