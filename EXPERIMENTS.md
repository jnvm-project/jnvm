# Artifact Evaluation : J-NVM

## Overview
* System Requirements (read)
* Getting Started (read)
* System configuration (5min)
* Building the sources (5min + download ~1GB)
* Running the benchmarks (10min human + TODOh compute)

## System Requirements
* Baremetal machine (not a VM) with root access
* 1x 128GB Optane DC-PMM
* Intel processor supporting `clwb` or `clflushopt` instructions.
  May be verified with `lspcu | grep "clwb\|clflushopt"`
* At least, 8 cores and 32GB of DRAM
* Linux system with kernel 4 or higher (at least 4.15 recommended)
* Docker

Although the benchmark applications can run without real PMEM and/or a
processor supporting the right instructions, the results obtained when using
emulated PMEM or a RAM filesystem would be significantly different due to the
large hardware difference between DRAM and Optane DC-PMM.

## Getting Started

### Overview

We evaluate J-NVM by running different application benchmarks such as YCSB and
a TPC-B like app on Infinispan, using various persistent cache stores.
We implemented a J-NVM infinispan cache store and a PCJ one and also use the
filesystem cache store to compare the performance of J-NVM relative to other
librairies (PCJ) or filesystems when used as the persistent layer of key-value
store applications.

### Contents

* [jnvm](https://github.com/jnvm-project/jnvm):
  The core of the framework, from the low-level internals to the high-level persistent data types and structures.
* [ptransformer-plugin](https://github.com/jnvm-project/ptransformer-plugin):
  Automated pojo class rewriting tool to generate J-NVM's persistent objects.
* [infinispan-cachestore-jnvm](https://github.com/jnvm-project/infinispan-cachestore-jnvm):
  Infinispan persistent cache store implemented for J-NVM objects with J-NVM collections.
* [ycsb](https://github.com/jnvm-project/YCSB):
  Fork of YCSB with patches to support J-NVM objects.
* [tpc-b](https://github.com/jnvm-project/tpcb):
  TPC-B like transaction application written with Infinispan for data persistence.
* [infinispan-cachestore-pcj](https://github.com/jnvm-project/infinispan-cachestore-pcj):
  Our infinispan persistent cache store implementation for PCJ objects with PCJ collections.
* [pcj](https://github.com/jnvm-project/pcj):
  Persistent Collection for Java, a persistent object library which relies on Intel's PMDK and JNI.
  Forked only to add compatibility with maven build system.
* [go-pmem](https://github.com/jnvm-project/go-pmem):
  PMEM extensions for Go.
  Forked to ease GC statistics gathering.
* [go-redis-pmem](https://github.com/jnvm-project/go-redis-pmem):
  Feature-poor redis server written for go-pmem extensions.
  Forked to add support for embedded mode (non-client/server operations - shared memory mode).
* [go-ycsb](https://github.com/jnvm-project/go-ycsb):
  YCSB implementation in Go.
  Forked to add bindings for our fork of go-redis-pmem.

## System configuration

Some manual steps are required before deploying and running the experiments.
We detail how to reach the expected configuration for Optane DC modules.
We also explain how to configure [NullFSVFS](https://github.com/abbbi/nullfsvfs),
a /dev/null-like filesystem that we use in one experiment.

### PMEM setup

First off, make sure the NV-DIMMs are configured in app-direct mode *not interleaved* in the machine's BIOS.
The goal is then to expose one nvdimm as a block device in `fsdax` mode, using
[ndctl](https://docs.pmem.io/persistent-memory/getting-started-guide/what-is-ndctl).
We will not cover how to [emulate PMEM](https://pmem.io/2016/02/22/pm-emulation.html) or create a tmpfs.

1. Check existing active namespaces in fsdax mode.

```
    $ ndctl list -Nv -m fsdax
[
  {
    "dev":"namespace0.0",
    "mode":"fsdax",
    "map":"dev",
    "size":133175443456,
    "uuid":"e880d0ea-9dc4-49f3-888d-1376c382cfaf",
    "raw_uuid":"eedc6ce9-c251-4f28-9b86-3cccbe498797",
    "sector_size":512,
    "blockdev":"pmem0",
    "numa_node":0
  }
]
```
In this sample output, we can see that a namespace in fsdax mode is accessible from `/dev/pmem0` and tied to numa node 0.
Skip step 2 if this is the kind of output you get.

2. Creating a fsdax namespace

Often this step involves understanding how the nv-dimms are currently configured.
Providing generic instructions is not easy. I will try to add some in the future.
For now, refer to the official ndctl documentation and seek live troubleshooting.

3. Creating and mounting the filesystem.

Instructions for pmem device `/dev/pmemX`, where X is the device you identified earlier.
```
    $ mkdir -p /pmemX
    $ mkfs.ext4 /dev/pmemX
    $ mount -o dax /dev/pmemX /pmemX
```
DAX mount is sucessful when
```
    $ dmesg | grep pmemX
[...] EXT4-fs (pmemX): DAX enabled. Warning: EXPERIMENTAL, use at your own risk
[...] EXT4-fs (pmemX): mounted filesystem with ordered data mode. Opts: dax
```

4. Keep in mind the numa node number and mountpoint of the pmem device.
They will be needed as parameters for the docker images.

### NullFSVFS

In the paper, figure 8, we use the [NullFSVFS](https://github.com/abbbi/nullfsvfs) additional filesystem for comparison.
Installation steps :
```
    $ git clone https://github.com/abbbi/nullfsvfs.git
    $ cd nullfsvfs
    $ make
    $ insmod nullfs.ko
    $ mkdir -p /blackhole
    $ mount -t nullfs none /blackhole
```

In the same figure, we also use a tmpfs. Make sure `/dev/shm` is indeed a tmpfs
with at least 8GB available.

## Building the sources

We packaged the different benchmark applications using docker containers.
For simplicity, we published pre-built docker images on dockerhub that may be directly used.
Fetch them all at once :
```
    $ ./bench.sh pull-all
```

We have plans to add in the near future complete detailed steps to manually build the sources
locally to run baremetal experiments, and indications for locally rebuilding
the docker images.

## Running the benchmarks

The benchmarks are all run from docker containers and all started with the
`bench.sh` driver script. They will run silently. For progress, you can check
either the docker log of the container or directly peek inside the `results`
directory where experiment logs, aggregated data and plotted figure will sit.

### Environment setup

Some ENV vars must be exported because the default might not be sane with your
system configuration and capabilities.
Please, indicate the mountpoints to the PMEM and other special filesystems.
It is also necessary to choose the numa node the application threads must be
restricted to. Ideally, should match the node to which the PMEM module
is attached.
For systems with small amounts of memory (lesser than 20GB), the max heap size
for Java should also be set - most experiments default to 20GB. Values smaller
than 8GB are untested.

```
#Set values matching your system properties
export PMEM_MOUNT="/pmem0"
export NUMA_NODE=0
export JHEAP_SIZE="20g"
#Only used for fig 8
export TMPFS_MOUNT="/dev/shm"
export NULLFS_MOUNT="/blackhole"
```

Verify your current env settings :
```
    $ ./bench.sh check-env
```

### Test runs

We provide another ENV var `EXP_PRESET` which can be used to select premade
experiment-wide settings. It currently supports two values `default` or `tiny`.
The `tiny` preset is especially useful to quickly run every experiment and
control whether everything is working properly. This preset simply sets down
considerably the number of records (dataset size) and operations for the experiments.
Use it this way:
```
    $ EXP_PRESET="tiny" ./bench.sh <EXPERIMENT_COMMAND>
```
When activated, every run should complete under 2seconds and so the total
compute time is around 20min for all experiments.
Bear in mind though, that with runs lasting less than 2secs, the results and
graphs won't match the values produced with the `default` preset which takes longer.
The compute times listed afterwards are the ones for the `default` preset when
not stated otherwise.

### Plotting the graphs

Graphs will be automatically generated at the end of experiment runs - no
actions are needed.
The figures will be available as png images under the results directory.

Yet, it is possible to generate graphs on demand by adding the `--no-run`
optional parameter :
```
    $ ./bench.sh --no-run <EXPERIMENT_COMMAND>
```
This simply skips running the benchmark and tries plotting the graph with
already available logs/data in the results directory.

### YCSB

We have 9 different experiments using YCSB, run them one by one
(description follows) or all at once (takes very long) with:
```
    $ ./bench.sh ycsb_all
```

At this stage, you might want to run the following to control that everything
is in proper working order:
```
    $ EXP_PRESET="tiny" ./bench.sh ycsb_all
```
This step should take just around 15min to run everything and generate graphs.
The graphs may be a bit distorted but if all PNG are non-empty and all data
points inside can be seen, then it is safe to proceed with the next (longer) runs.

#### YCSB Throughput (fig 7)
(TODOmin compute)
Run the experiment:
```
    $ ./bench.sh ycsb_throughput
```

#### Filesystem marshal cost (fig 8)
(TODOmin compute)
Run the experiment:
```
    $ ./bench.sh ycsb_marshalling
```

#### Volatile/Persistent cache ratio impact (fig 9.a)
(TODOmin compute)
Run the experiment:
```
    $ ./bench.sh ycsb_cacheratio
```

#### Record count impact (fig 9.b)
(TODOmin compute)
Run the experiment:
```
    $ ./bench.sh ycsb_recordcount
```

#### Field count per record impact (fig 9.c)
(TODOmin compute)
Run the experiment:
```
    $ ./bench.sh ycsb_fieldcount
```

#### Field length per record impact (fig 9.d)
(TODOmin compute)
Run the experiment:
```
    $ ./bench.sh ycsb_recordsize
```

#### Concurrent throughput (fig 10)
(TODOmin compute)
Run the experiment:
```
    $ ./bench.sh ycsb_concurrent
```

#### J-NVM PDT vs JDK's volatile data types (fig 12)
(TODOmin compute)
Run the experiment:
```
    $ ./bench.sh ycsb_pdt
```

#### Java's GC vs Big data heaps (fig 1)
(TODOmin compute)
**Attention**, this experiment will occupy roughly 80GB with on-heap data, will
set the max heap size to 100GB at most, and may not run with less than 128GB
memory installed on the machine.
Unfortunately, it is not possible to reproduce the results with smaller size of data.
Skip if installed memory is not large enough.
It is also rather long to run (2h or more).
Run the experiment:
```
    $ ./bench.sh ycsb_heapsize
```

### TPC-B
(20min compute)

Run the experiment:
```
    $ ./bench.sh tpcb
```

During our testing of the packaged experiment, we had some serious
bottlenecking from the client when it was running in a docker container.
The figure generated by the above command may be seriously off for this reason.
I found no solution yet, so below are the steps to run the client baremetal and
the server from a docker container, which is the configuration we used for the
paper. It is far from ideal since it requires manual steps, we will have to fix
this soon. Here are the steps :

1. make sure to install the following : `bash`, `coreutils` (date), `grep`, `gawk`, `curl`, `git`, `gnuplot`
2. run `git clone https://github.com/jnvm-project/tpcb.git && cd tpcb/`
3. run `docker pull gingerbreadz/transactions:latest`
4. have PMEM_MOUNT, NUMA_NODE, JHEAP_SIZE env vars set to your proper values
5. `mkdir -p results/tpcb && export EXP_OUTDIR=results/tpcb`
6. run `/src/test/bin/fault.sh` and wait
7. use `bench.sh --no-run tpcb` to generate the plots

### GO-PMEM
(TODOmin compute)

Run the experiment:
```
    $ ./bench.sh gopmem
```

### J-NVM vs native (C) µbenchmarks

The micro-benchmarks from table 3 in the paper.
I had no time to package them yet.
I will set up the sources online and package the experiment once I get back in September.

## Results
Results will be generated in a `results/` folder at the root of the repository.
It will contain, per experiment, data file(s) and a figure matching the ones
found in the paper.

## Replicating Results
The machine used for the paper was a server with 4 sockets running Debian 10
(kernel 4.19) with 4 Intel(R) Xeon(R) Gold 6230 CPU @ 2.10GHz, 128GB DRAM
(32GB per socket) and 512GB Optane DC-PMM (1x 128GB installed per socket).
