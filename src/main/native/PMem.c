#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

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
    return mmap(NULL, length, PROT_EXEC|PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0);
}

static void free_pmem_root(void *addr, size_t length) {
    munmap(addr, length);
    close(fd);
}
