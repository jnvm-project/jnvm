set terminal png
set output '/results/ycsb_distribution.png'

load "/ycsb/plot/styles.inc"

set title ""
set auto x
set style data histogram
set style fill solid border -1
set boxwidth 0.9
set border 1

set xtics offset 0,-1 rotate by -45 left nomirror
set ytics nomirror

set key above right samplen 2 width 15 spacing 2 vertical maxrows 3
set key tmargin

set lmargin 10                                                               
set rmargin 5                                                               
set tmargin 5                                                            
set bmargin 10

set ylabel "Latency ($\\mu$s)" offset -2,0
set xlabel "\\# fields" offset 0,-4

set logscale y
set yrange [0:100000]

unset key
unset ytics
unset ylabel

plot \
"< cat /results/data/exp3.distribution.dat | grep read | grep \"nvm\" | awk -F, '{printf \"%g %2.2f\\n\", $3, ($10/1000)}'"  using ($2):xtic(1) title "\\SYS" ls 1, \
"< cat /results/data/exp3.distribution.dat | grep read | grep \"infinispan,\" | awk -F, '{printf \"%g %2.2f\\n\", $3, ($10/1000)}' "  using ($2):xtic(1) title "DAX" ls 2, \
"< cat /results/data/exp3.distribution.dat | grep update | grep \"nvm\" | awk -F, '{printf \"%g %2.2f\\n\", $3, ($10/1000)}'"  using ($2):xtic(1) title "\\SYS" ls 1 fs pattern 1, \
"< cat /results/data/exp3.distribution.dat | grep update | grep \"infinispan,\" | awk -F, '{printf \"%g %2.2f\\n\", $3, ($10/1000)}' "  using ($2):xtic(1) title "DAX" ls 2 fs pattern 1
