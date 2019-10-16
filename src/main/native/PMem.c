#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

#include "PMem.h" //Generated by javah

#define MAX_BUF_SIZE 1024*1024*1024UL

static int fd;

static void zero_out_file(int fd, size_t length) {
    int chunk_length = (length < MAX_BUF_SIZE) ? length : MAX_BUF_SIZE;
    size_t written = 0;

    char *zero = calloc(chunk_length, sizeof(char));
    while( (written += write(fd, zero, chunk_length * sizeof(char))) < length);
    fsync(fd);
    free(zero);
}

static char *open_pmem_root(const char *path, size_t length) {
    if((fd = open(path, O_RDWR, 0777)) < 0) {
        int errnum = errno;
        if(errnum == ENOENT) {
            if((fd = open(path, O_RDWR|O_CREAT|O_EXCL, 0777)) >= 0)
                zero_out_file(fd, length);
        } else {
            perror("open_pmem_root: ");
            exit(EXIT_FAILURE);
        }
    }
    int flags = MAP_SHARED_VALIDATE | MAP_SYNC;
    char *addr = mmap(NULL, length, PROT_READ|PROT_WRITE, flags, fd, 0);
    if(addr == MAP_FAILED) {
        int errnum = errno;
        if(errnum == EOPNOTSUPP) {
            flags = MAP_SHARED;
            addr = mmap(NULL, length, PROT_READ|PROT_WRITE, flags, fd, 0);
        } else {
            perror("open_pmem_root: ");
            exit(EXIT_FAILURE);
        }
    }
}

static void free_pmem_root(void *addr, size_t length) {
    munmap(addr, length);
    close(fd);
}

JNIEXPORT jlong JNICALL Java_eu_telecomsudparis_jnvm_PMem_openPmemRoot
        (JNIEnv *env, jobject jobj, jstring jpath, jlong jsize) {
    const char *path = NULL;
    char *root = NULL;

    if((path = (*env)->GetStringUTFChars(env, jpath, NULL)) == NULL)
        return -2;
    if((root = open_pmem_root(path, jsize)) == NULL)
        return -1;
    (*env)->ReleaseStringUTFChars(env, jpath, path);

    return (jlong) root;
}

JNIEXPORT void JNICALL Java_eu_telecomsudparis_jnvm_PMem_freePmemRoot
        (JNIEnv *env, jobject jobj, jlong jaddr, jlong jsize) {
    free_pmem_root((void *) jaddr, jsize);
}
