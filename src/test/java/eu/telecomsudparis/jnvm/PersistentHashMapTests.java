package eu.telecomsudparis.jnvm.api.util.persistent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

import eu.telecomsudparis.jnvm.api.PMemPool;
import eu.telecomsudparis.jnvm.api.util.persistent.PersistentHashMap;
import eu.telecomsudparis.jnvm.api.jpa.SampleObject;

@TestMethodOrder(OrderAnnotation.class)
class PMemPoolTests {

    private final static String PMEM_FILE="/tmp/pMemTests";
    private final static long POOL_SIZE=1024*1024L;

    private final static int MULTI_PUT_LENGTH = 100;
    private final static String PERSISTENT_VALUE =
        "This is an exemple string used as a payload for this simple unit test "
        + "class, thus it should be of sufficient size for the test to be "
        + "meaningful";

    private PMemPool pmemPool;
    private PersistentHashMap<Integer, byte[]> pmemMap;


    @BeforeEach
    void setup() {
        Assumptions.assumeFalse( pmemMap != null );

        pmemPool = new PMemPool( POOL_SIZE, PMEM_FILE );
        pmemMap = new PersistentHashMap<Integer, byte[]> ( pmemPool );
    }

    @Test
    @Order(1)
    void prepare() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );

        pmemPool.clear( true );
        pmemMap.clear();

        Assertions.assertEquals( pmemMap.size(), 0 );
    }

    @Test
    @Order(2)
    void put() {
        pmemMap.put( 0, PERSISTENT_VALUE.getBytes() );

        Assertions.assertEquals( pmemMap.size(), 1 );
    }

    @Test
    @Order(3)
    void get() {
        Assumptions.assumeTrue( pmemMap.size() > 0 );
        String expected = PERSISTENT_VALUE;

        byte[] read = pmemMap.get( 0 );

        Assertions.assertEquals( expected, new String( read ) );
    }

    @Test
    @Order(4)
    void remove() {
        Assumptions.assumeTrue( pmemMap.size() > 0 );

        int prevSize = pmemMap.size();
        pmemMap.remove( 0 );

        Assertions.assertEquals( pmemMap.size(), prevSize - 1 );
    }

    @Test
    @Order(5)
    void multiPut() {
        pmemMap.clear();

        for(int i=0; i < MULTI_PUT_LENGTH; i++) {
            byte[] value = ( Integer.toString(i) + PERSISTENT_VALUE ).getBytes();
            pmemMap.put( i, value );
        }

        Assertions.assertEquals( pmemMap.size(), MULTI_PUT_LENGTH );
    }

    @Test
    @Order(6)
    void multiGet() {
        Assumptions.assumeTrue( pmemMap.size() == MULTI_PUT_LENGTH );

        for(int i=0; i < MULTI_PUT_LENGTH; i++) {
            String expected = Integer.toString( i ) + PERSISTENT_VALUE;
            byte[] read = pmemMap.get( i );

            Assertions.assertEquals( expected, new String( read ) );
        }
    }

    @Test
    @Order(7)
    void clear() {
        Assumptions.assumeTrue( !pmemMap.isEmpty() );

        pmemMap.clear();

        Assertions.assertEquals( pmemMap.size(), 0 );
    }

    @AfterEach
    void tearDown() {
        Assumptions.assumeTrue( pmemPool.isLoaded() );

        pmemMap = null;
        pmemPool.close();

        Assertions.assertFalse( pmemPool.isLoaded() );
    }

}
