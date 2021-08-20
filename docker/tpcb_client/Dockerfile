FROM docker:20.10

ENV PMEM_MOUNT=/pmem0
ENV NUMA_NODE=0
ENV JHEAP_SIZE=20g
ENV EXP_OUTDIR=/results
ENV NAME=tpcb

VOLUME ["$EXP_OUTDIR"]

RUN apk add --no-cache bash coreutils grep gawk curl gnuplot git
RUN git clone https://github.com/jnvm-project/tpcb.git $NAME

WORKDIR $NAME

COPY entrypoint.sh .
COPY parse.sh .
COPY plot.gp .
COPY styles.inc .
RUN chmod +x *.sh

CMD ["bash", "entrypoint.sh"] 
