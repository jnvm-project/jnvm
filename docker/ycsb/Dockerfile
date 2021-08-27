FROM gingerbreadz/jnvm-jdk:prebuilt

ENV PMEM_MOUNT=/pmem0
ENV NUMA_NODE=0
ENV JHEAP_SIZE=20g
ENV EXP_OUTDIR=/ycsb/exp/out/exp0.exectime.ref
ENV EXP_NAME=exp0_exectime
ENV NAME=ycsb

ARG YCSB_PATH

VOLUME ["/ycsb/exp/out"]

RUN set -eux;\
        apt-get update;\
        apt-get install -y --no-install-recommends numactl gawk gnuplot-nox git;\
        apt-get install -y --no-install-recommends libpmem1 libpmemblk1 libpmemlog1 libpmemobj1 libpmempool1;\
        rm -rf /var/lib/apt/lists/*
        #apt-get install -y --no-install-recommends numactl maven gawk gnuplot git;\
#RUN git clone https://github.com/jnvm-project/jnvm.git &&\
#        cd /jnvm && mvn clean install
#RUN git clone https://github.com/jnvm-project/infinispan-cachestore-jnvm.git &&\
#        cd /infinispan-cachestore-jnvm && git checkout ispn9.4.17 && mvn clean install
#RUN git clone https://github.com/jnvm-project/ycsb.git &&\
#        cd /ycsb &&\
#        mvn -pl infinispan  -am clean package -P source-run -Dmaven.test.skip=true\
#        mvn -pl jnvm  -am clean package -P source-run -Dmaven.test.skip=true

COPY $YCSB_PATH /$NAME

WORKDIR $NAME

COPY entrypoint.sh .
COPY plot plot/
RUN chmod +x plot/*.sh

CMD ["bash", "entrypoint.sh"] 
