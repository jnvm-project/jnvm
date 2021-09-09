set terminal png
set output outdir.'/exp0.exectime.ref/ycsb_executiontime.png'

load "/ycsb/plot/styles.inc"

set title ""
set auto x
set style data histogram
set style histogram cluster gap 1
set style fill solid border -1
set boxwidth 0.9
set border 3

set style fill solid border rgb "black"
set auto x

set xtics offset 0,-1 nomirror
set ytics nomirror

set key above right samplen 2 width 6 spacing 1 vertical maxrows 1
set key at 5,800

set lmargin 9
set rmargin 5
set tmargin 17
set bmargin 3

set grid ytics
set yrange [0:600]
set ytics 100
set ylabel "Throughput\n(Kops/s)" offset -1,0

unset xlabel

plot \
"< for w in a b c d f; do echo -n $w\" \" | tr a-z A-Z; for s in infinispan, infinispan-jnvm, infinispan-pcj, infinispan-jpfa; do cat ${EXP_OUTDIR}/exp0.exectime.ref/data/exp0.exectime.dat | grep workload${w} | grep transaction | grep ${s} | grep \"run,\" | awk -F, '{printf $11\" \"}'; done | awk '{for (i=1; i<=NF; i++) printf 10*1e12/$i\" \";print \"\"}'; done" using ($3):xtic(1) title "\\SYSPDT" ls 1, \
"< for w in a b c d f; do echo -n $w\" \" | tr a-z A-Z; for s in infinispan, infinispan-jnvm, infinispan-pcj, infinispan-jpfa; do cat ${EXP_OUTDIR}/exp0.exectime.ref/data/exp0.exectime.dat | grep workload${w} | grep transaction | grep ${s} | grep \"run,\" | awk -F, '{printf $11\" \"}'; done | awk '{for (i=1; i<=NF; i++) printf 10*1e12/$i\" \";print \"\"}'; done" using ($5):xtic(1) title "\\SYSFA" ls 1 fs pattern 2, \
"< for w in a b c d f; do echo -n $w\" \" | tr a-z A-Z; for s in infinispan, infinispan-jnvm, infinispan-pcj, infinispan-jpfa; do cat ${EXP_OUTDIR}/exp0.exectime.ref/data/exp0.exectime.dat | grep workload${w} | grep transaction | grep ${s} | grep \"run,\" | awk -F, '{printf $11\" \"}'; done | awk '{for (i=1; i<=NF; i++) printf 10*1e12/$i\" \";print \"\"}'; done" using ($2):xtic(1) title "\\FS" ls 3, \
"< for w in a b c d f; do echo -n $w\" \" | tr a-z A-Z; for s in infinispan, infinispan-jnvm, infinispan-pcj, infinispan-jpfa; do cat ${EXP_OUTDIR}/exp0.exectime.ref/data/exp0.exectime.dat | grep workload${w} | grep transaction | grep ${s} | grep \"run,\" | awk -F, '{printf $11\" \"}'; done | awk '{for (i=1; i<=NF; i++) printf 10*1e12/$i\" \";print \"\"}'; done" using ($4):xtic(1) title "\\PCJ" ls 4

# 10.1e6 operation
# time in ns => /1e9 in s
# 18-3 => k.op/s

