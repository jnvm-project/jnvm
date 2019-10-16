package eu.telecomsudparis.jnvm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

import eu.telecomsudparis.jnvm.PMem;

class PMemTests {

    private sun.misc.Unsafe unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();

    private final static String PMEM_FILE="/pmem0/pMemTests";
    private final static long POOL_SIZE=1024*1024L;
    private final static int PERSISTENT_VALUE=1;

    private long base;

    @BeforeEach
    void setup() {
        base = PMem.openPmemRoot(PMEM_FILE, POOL_SIZE);
    }

    @Test
    void writeInt() {
        Assumptions.assumeTrue(base >= 0);

        unsafe.putInt(base, PERSISTENT_VALUE);
        Assertions.assertEquals(PERSISTENT_VALUE, unsafe.getInt(base));
    }

    @Test
    void readInt() {
        Assumptions.assumeTrue(base >= 0);

        Assertions.assertEquals(PERSISTENT_VALUE, unsafe.getInt(base));
    }

    @AfterEach
    void tearDown() {
        PMem.freePmemRoot(base, POOL_SIZE);
    }

}
