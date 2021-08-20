set terminal png
set output '/results/tpcb.png'

load "/tpcb/styles.inc"

set title ""
set auto x
set border 3

set key inside right samplen 2 width 3 spacing 1 vertical maxrows 1

set xtics nomirror
set yrange [0:15]
set xrange [0:120]
set ytics nomirror
set grid ytics

set ylabel "Throughput (Kops/s)"
set xlabel "Time (s)"

plot \
"< /tpcb/parse.sh /results/mem.log" title "\\Volatile" smooth csplines with linespoints ls 7 ps 0.5, \
"< /tpcb/parse.sh /results/jnvm.log" title "\\SYSFA" smooth csplines with linespoints ls 1 ps 0.5, \
"< /tpcb/parse.sh /results/sfs.log" title "\\FS" smooth csplines with linespoints ls 2 ps 0.5
