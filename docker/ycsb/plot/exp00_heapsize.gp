set terminal png
set output outdir.'/exp00.heapsize.ref/ycsb_gc.png'

load "/ycsb/plot/styles.inc"

set tmargin 3
set bmargin 10

set title ""
set auto x
set size 1, 1
set border 3

set key above right samplen 2 width 3 spacing 1 vertical maxrows 1
unset key

set xtics nomirror
set format x "$10^%T$"
set logscale x 10
set yrange [0.95:1]
#set ytics 0.99,0.005,1
#set yrange [0:1]
#set ytics 0,0.2,1
set xrange [60:10000]
#set xrange [20:10000]
set ytics nomirror
set grid ytics

set ylabel "CDF"
set xlabel "Latency (\\microsecond{})" offset 0,-.25

set lmargin 5
set rmargin 35
set tmargin 15
set bmargin 5

set key inside bottom right samplen 2 width 3 spacing 1 vertical maxrows 3

plot \
"< /ycsb/plot/gc.sh 1" title "1\\%" smooth bezier ls 6, \
"< /ycsb/plot/gc.sh 10" title "10\\%" smooth bezier ls 5 dt 2, \
"< /ycsb/plot/gc.sh 100" title "100\\%" smooth bezier ls 7
