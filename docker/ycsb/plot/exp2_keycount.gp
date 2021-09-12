set terminal png
set output outdir.'/exp2.keycount.ref/ycsb_keycount.png'

load "/ycsb/plot/styles.inc"

set title ""
set auto x
set style data histogram
set style fill solid border -1
set boxwidth 0.9
set border 1

set xtics offset 0,-1 rotate by -45 left nomirror
set grid ytics lt 0 lw 1 lc rgb "#000000"

set key above right samplen 2 width 18 spacing 2 vertical maxrows 2
set key inside

set lmargin 10                                                               
set rmargin 5                                                               
set tmargin 5                                                            
set bmargin 10

set ylabel "Latency ($\\mu$s)" offset -2,0
set xlabel "\\# records" offset 0,-4.7

set logscale y
set yrange [0:10000]

unset ytics
unset ylabel

plot \
"< cat ${EXP_OUTDIR}/exp2.keycount.ref/data/exp2.keycount.dat | grep read_lat | grep workloada | grep \"run,\" | grep -v \",5000000,\" | grep nvm | awk -F \",\" '{print $2\" \"($NF/1000)}' | sort -n | awk '{printf \"%1.2g %2.2f\\n\" , $1 , $2}' | sed s/1e\+0/10^/g | awk '{print \"\$\"$1\"\$ \"$2}'" using ($2):xtic(1) title "\\aread (\\SYSPDT)" ls 1, \
"< cat ${EXP_OUTDIR}/exp2.keycount.ref/data/exp2.keycount.dat | grep \"run,\" | grep read_lat | grep workloada | grep -v \",5000000,\" | grep \"infinispan,\" | awk -F \",\" '{print $2\" \"($NF/1000)}' | sort -n | awk '{printf \"%1.2g %2.2f\\n\" , $1 , $2}' | sed s/1e\+0/10^/g | awk '{print \"\$\"$1\"\$ \"$2}'" using ($2):xtic(1) title "\\aread (\\FS)" ls 2, \
"< cat ${EXP_OUTDIR}/exp2.keycount.ref/data/exp2.keycount.dat | grep update_lat | grep workloada | grep \"run,\" | grep -v \",5000000,\" |grep nvm | awk -F \",\" '{print $2\" \"($NF/1000)}' | sort -n | awk '{printf \"%1.2g %2.2f\\n\" , $1 , $2}' | sed s/1e\+0/10^/g | awk '{print \"\$\"$1\"\$ \"$2}'" using ($2):xtic(1) title "\\update (\\SYSPDT)" ls 1 fs pattern 1, \
"< cat ${EXP_OUTDIR}/exp2.keycount.ref/data/exp2.keycount.dat | grep \"run,\" | grep update_lat | grep workloada | grep -v \",5000000,\" |grep \"infinispan,\" | awk -F \",\" '{print $2\" \"($NF/1000)}' | sort -n | awk '{printf \"%1.2g %2.2f\\n\" , $1 , $2}' | sed s/1e\+0/10^/g | awk '{print \"\$\"$1\"\$ \"$2}'" using ($2):xtic(1) title "\\update (\\FS)" ls 2 fs pattern 1
