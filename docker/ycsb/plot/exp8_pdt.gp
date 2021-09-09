set terminal png
set output outdir.'/exp8.pdt.ref/ycsb_pdt.png'

load "/ycsb/plot/styles.inc"

set title ""
set style data histogram
set style histogram rowstacked
#set style histogram cluster gap 1
set style fill solid border -1
set boxwidth 0.9
set border 3

set style fill solid border rgb "black"

set key autotitle columnheader
set key inside top left samplen 2 width 1 vertical maxrows 2

set xtics nomirror
set ytics nomirror

set auto x
unset xlabel
unset xtics

set auto y
set yrange [0:15]
set grid ytics
set ylabel "Completion time (s)"

set label "Volatile" at 2,10
set label "J-PDT" at 3.8,10
set arrow from 3,4.5 to 2.5,9.1 nohead lc black lw 2
set arrow from 4,6.5 to 4.3,9.1 nohead lc black lw 2

plot \
  newhistogram "Blackhole" offset 0,-1.4, for [i=2:5] "< /ycsb/plot/pdt.sh blackhole blackhole-offheap" using i:xtic(1) ls 7 fs pattern i notitle, \
  newhistogram "HashMap" offset 0,-1.4, for [i=2:5] "< /ycsb/plot/pdt.sh vhmap rhmap" using i:xtic(1) ls 7 fs pattern i notitle, \
  newhistogram "TreeMap" offset 0,-1.4, for [i=2:5] "< /ycsb/plot/pdt.sh vtmap rtmap" using i:xtic(1) ls 7 fs pattern i notitle, \
  newhistogram "SkipListMap" offset 0,-1.4, for [i=2:5] "< /ycsb/plot/pdt.sh vslmap rslmap" using i:xtic(1) ls 7 fs pattern i
