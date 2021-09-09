set terminal png
set output outdir.'/exp7.marshalling.ref/ycsb_marshalling.png'

load "/ycsb/plot/styles.inc"

set title ""
set style data histogram
set style histogram cluster gap 1
set style fill solid border -1
set boxwidth 0.9
set border 3

set style fill solid border rgb "black"

set key above right samplen 2 width 5 spacing 1 vertical maxrows 1

set xtics nomirror
set ytics nomirror

set auto x
#set xrange [0:1000]
#set xtics 200
set xlabel "Record size (KB)"

set auto y
set grid ytics
set yrange [0:210]
set ytics 50
set ylabel "Completion\ntime (s)"


plot \
  "< cat ${EXP_OUTDIR}/exp7.marshalling.ref/data/exp7.marshalling.dat | grep 'transaction,' | grep 'r1,' | grep none | awk -F, '{if ($1/$2 == 1) printf $4/100\" \"$20/1e9\"\\n\"}' | sort -n" using 2:xtic(1) title "\\Volatile" ls 7, \
    "< cat ${EXP_OUTDIR}/exp7.marshalling.ref/data/exp7.marshalling.dat | grep 'transaction,' | grep 'r1,' | grep nullfsvfs | awk -F, '{if ($1/$2 == 1) printf $4/100\" \"$20/1e9\"\\n\"}' | sort -n" using 2:xtic(1) title "\\NULLFS" ls 3 fs pattern 2, \
  "< cat ${EXP_OUTDIR}/exp7.marshalling.ref/data/exp7.marshalling.dat | grep 'transaction,' | grep 'r1,' | grep tmpfs | awk -F, '{if ($1/$2 == 1) printf $4/100\" \"$20/1e9\"\\n\"}' | sort -n" using 2:xtic(1) title "\\TMPFS" ls 3 fs pattern 4, \
      "< cat ${EXP_OUTDIR}/exp7.marshalling.ref/data/exp7.marshalling.dat | grep 'transaction,' | grep 'r1,' | grep pmem | awk -F, '{if ($1/$2 == 1) printf $4/100\" \"$20/1e9\"\\n\"}' | sort -n" using 2:xtic(1) title "\\FS" ls 3

# 3e6 operation
# 1e6 recordcount
# 100% cachesize
# 10 fieldcount
# fieldlength 100 -> 1000
