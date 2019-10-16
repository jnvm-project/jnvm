package eu.telecomsudparis.jnvm.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

import eu.telecomsudparis.jnvm.api.PMemPool;

@TestMethodOrder(OrderAnnotation.class)
class PMemPoolTests {

    private sun.misc.Unsafe unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();

    private final static String PMEM_FILE="/pmem0/pMemTests";
    private final static long POOL_SIZE=1024*1024L;

    private final static int MULTI_PUT_LENGTH = 100;
    private final static String PERSISTENT_VALUE =
        "This is an exemple string used as a payload for this simple unit test "
        + "class, thus it should be of sufficient size for the test to be "
        + "meaningful";

    private PMemPool pmemPool = new PMemPool(POOL_SIZE, PMEM_FILE);

    @BeforeEach
    void setup() {
        Assumptions.assumeFalse( pmemPool.isLoaded() );

        pmemPool.open();

        Assertions.assertTrue( pmemPool.isLoaded() );
    }

    @Test
    @Order(1)
    void prepare() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );

        pmemPool.clear( true );

        Assertions.assertEquals( pmemPool.getSize(), 0 );
    }

    @Test
    @Order(2)
    void put() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );
        byte[] value = PERSISTENT_VALUE.getBytes();

        pmemPool.put( value, 0 );

        Assertions.assertEquals( pmemPool.getSize(), 1 );
    }

    @Test
    @Order(3)
    void get() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );
        Assumptions.assumeTrue( pmemPool.getSize() > 0 );
        String expected = PERSISTENT_VALUE;
        byte[] read = new byte[expected.getBytes().length];

        pmemPool.get( read, 0 );

        Assertions.assertEquals(expected, new String(read) );
    }

    @Test
    @Order(4)
    void remove() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );
        Assumptions.assumeTrue( pmemPool.getSize() > 0 );

        long prevSize = pmemPool.getSize();
        pmemPool.remove( 0 );

        Assertions.assertEquals( pmemPool.getSize(), prevSize - 1 );
    }

    @Test
    @Order(5)
    void multiPut() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );

        for(int i=0; i < MULTI_PUT_LENGTH; i++) {
            byte[] value = ( Integer.toString(i) + PERSISTENT_VALUE ).getBytes();
            pmemPool.put( value );
        }

        Assertions.assertEquals( pmemPool.getSize(), MULTI_PUT_LENGTH );
    }

    @Test
    @Order(6)
    void multiGet() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );

        pmemPool.rewind();
        for(int i=0; i < MULTI_PUT_LENGTH; i++) {
            String expected = Integer.toString(i) + PERSISTENT_VALUE;
            byte[] read = new byte[expected.getBytes().length];
            pmemPool.get( read );

            Assertions.assertEquals( expected, new String(read) );
        }
    }

    @Test
    @Order(7)
    void clear() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );
        Assumptions.assumeTrue( pmemPool.getSize() > 0 );

        pmemPool.clear();

        Assertions.assertEquals( pmemPool.getSize(), 0 );
    }

    @AfterEach
    void tearDown() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );

        pmemPool.close();

        Assertions.assertFalse( pmemPool.isLoaded() );
    }

}
