set terminal png
set output outdir.'/exp00.heapsize.ref/ycsb_gc2.png'

load "/ycsb/plot/styles.inc"

set title ""
set key invert reverse Left outside
set key autotitle columnheader
set style data histogram
set style histogram rowstacked
set style fill solid border -1
set boxwidth 0.75

unset key

x = 1.7
y = 28
dx = 0.5
dy = 2
set label 'gc' at x,y rotate by 90 offset 0.8,-1.7
set style rectangle fc ls 5 fs border 0
set object rectangle from x,y to x+dx,y+dy

x = 2.2
set label 'compute' at x,y rotate by 90 offset 0.8,-4
set style rectangle fc ls 6 fs border 0
set object rectangle from x,y to x+dx,y+dy

set xtic rotate by 90 nomirror offset 0,-3.2
set y2tics rotate by 90
set y2tics 0, 10, 30
unset ytics
set yrange [0:30]
set y2range [0:30]
set y2label "Completion time (min)" offset -1.2,0 rotate by 90

set border 9

set lmargin 40
set rmargin 12
set tmargin 10
set bmargin 5

plot \
  "< /ycsb/plot/gc2.sh" using 3:xtic(1) ls 6, '' using 6 ls 5
