set terminal png
set output '/results/ycsb_concurrent.png'

load "/ycsb/plot/styles.inc"

if (!exists("MP_LEFT"))   MP_LEFT = .1
if (!exists("MP_RIGHT"))  MP_RIGHT = .95
if (!exists("MP_BOTTOM")) MP_BOTTOM = 0.19
if (!exists("MP_TOP"))    MP_TOP = 0.82
if (!exists("MP_xGAP"))   MP_xGAP = 0.08
if (!exists("MP_yGAP"))   MP_yGAP = 0.1

set multiplot layout 1,2 columnsfirst \
              margins screen MP_LEFT, MP_RIGHT, MP_BOTTOM, MP_TOP spacing screen MP_xGAP, MP_yGAP

#set logscale y
# set logscale x
set xrange [1:20]
set xtics (1,5,10,15,20)
#set yrange [0.01:3]
set xtics nomirror
set ytics nomirror

unset key

set ylabel "Throughput (Mops/s)" offset 0.6,0
set label 1 at 10, 1 '{\sf YCSB-A}'
set yrange [0:1.1]
set ytics 0,0.2,1

plot \
"< cat /results/data/exp5.concurrent.dat | grep transaction | grep loada | grep run | grep nvm | awk -F, '{printf \"%d %f\\n\", $5,($4*1e-6)/($11*1e-9)}' | sort -n" using 1:2 title "\\SYSPDT" with linespoints ls 1 pointsize .7, \
"< cat /results/data/exp5.concurrent.dat | grep transaction | grep loada | grep run | grep -e ^100000, | awk -F, '{printf \"%d %f\\n\", $5,($4*1e-6)/($11*1e-9)}' | sort -n" using 1:2 title "\\FS" with linespoints ls 2 pointsize .7, \
"< cat /results/data/exp5.concurrent.dat | grep transaction | grep loada | grep run | grep -e ^1000000, | awk -F, '{printf \"%d %f\\n\", $5,($4*1e-6)/($11*1e-9)}' | sort -n" using 1:2 title "Volatile"with linespoints ls 7 pointsize .7

set xlabel "\\# threads" offset -15,0.2
unset ylabel
set label 1 at 10, 2.28 '{\sf YCSB-C}'
set yrange [0:2.5]
set ytics 0,0.5,2

set key above right samplen 2 width 2 spacing 2 vertical maxrows 1
set key tmargin

plot \
"< cat /results/data/exp5.concurrent.dat | grep transaction | grep loadc | grep run | grep nvm | awk -F, '{printf \"%d %f\\n\", $5,($4*1e-6)/($11*1e-9)}' | sort -n" using 1:2 title "\\SYSPDT" with linespoints ls 1 pointsize .7, \
"< cat /results/data/exp5.concurrent.dat | grep transaction | grep loadc | grep run | grep -e ^100000, | awk -F, '{printf \"%d %f\\n\", $5,($4*1e-6)/($11*1e-9)}' | sort -n" using 1:2 title "\\FS" with linespoints ls 2 pointsize .7, \
"< cat /results/data/exp5.concurrent.dat | grep transaction | grep loadc | grep run | grep -e ^1000000, | awk -F, '{printf \"%d %f\\n\", $5,($4*1e-6)/($11*1e-9)}' | sort -n" using 1:2 title "Volatile"with linespoints ls 7 pointsize .7

unset ylabel
unset xlabel
#unset ytics

set label 1 at 10, 0.68 '{\sf Update only}'
set yrange [0:0.75]
set ytics 0,0.2,0.7

unset multiplot