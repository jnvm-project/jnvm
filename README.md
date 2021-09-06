# JNVM

## Overview

J-NVM is a framework to efficiently access Non-Volatile Main Memory (NVMM) in
Java. J-NVM offers a natural interface to persist plain Java objects using
failure-atomic blocks. This interface relies internally on proxy objects that
intermediate direct off-heap access to NVMM.
The framework also provides a library of persistent data types that resist
reboots and power failures.

The [experiments page](EXPERIMENTS.md) lists evaluations of J-NVM,
where it is compared to other available interfaces with NVMM in Java using YCSB
and a TPC-B-like application benchmark.

## Build

### Prerequisites

The framework currently requires a patched Java Development Kit (JDK) version 8.
The [patches](patches) apply onto `jdk8u232-b03` changeset `c5ca527b0afd`.

### Steps

    mvn clean install

## Usage

Add the followings to your application's pom.xml

    <dependency>
      <groupId>eu.telecomsudparis.jnvm</groupId>
      <artifactId>jnvm</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
