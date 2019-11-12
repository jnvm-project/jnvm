package eu.telecomsudparis.jnvm.offheap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

import eu.telecomsudparis.jnvm.jpa.OnHeapFourLongs;
import eu.telecomsudparis.jnvm.jpa.OffHeapFourLongs;

@TestMethodOrder(OrderAnnotation.class)
class OffHeapTests {

    private final static int MULTI_PUT_LENGTH = 100;

    //Emulate OffHeap state logic to test OffHeap class
    private static class HeapStateUtils {
        private static long offsets[] = new long[ MULTI_PUT_LENGTH ];
        private static long heapSize;
        private static long heapTop;

        private static long size() { return heapSize; }
        private static long top() { return heapTop; }
        private static long offset(int idx) { return offsets[ idx ]; }

        private static void initHeapState() {
            heapSize = OffHeap.getAllocator().size();
            heapTop = OffHeap.getAllocator().top();
            for(int i=0; i < MULTI_PUT_LENGTH; i++) {
                offsets[i] = -1;
            }
        }

        private static void recordNewInstance(OffHeapObjectHandle ohoh, int idx) {
            offsets[ idx ] = ohoh.getOffset();
            if( heapSize >= heapTop ) {
                heapTop += OffHeap.getAllocator().blockFromOffset( ohoh.getOffset() ).size();
            }
            heapSize += OffHeap.getAllocator().blockFromOffset( ohoh.getOffset() ).size();
        }

        private static void recordDeletedInstance(OffHeapObjectHandle ohoh, int idx) {
            offsets[ idx ] = -1;
            heapSize -= OffHeap.getAllocator().blockFromOffset( ohoh.getOffset() ).size();
        }
    }

    @BeforeAll
    static void setupAll() {
        //TODO Open HEAP Factory
        HeapStateUtils.initHeapState();
    }

    @BeforeEach
    void setup() {
        //TODO Open HEAP
    }

    @Test
    @Order(1)
    void newInstance() {
        Assumptions.assumeTrue( OffHeap.getAllocator().size() == 0 );
        OnHeapFourLongs expected = new OnHeapFourLongs(0L, 1L, 2L, 3L);

        //Constructor
        OffHeapFourLongs o = new OffHeapFourLongs(0L, 1L, 2L, 3L);
        HeapStateUtils.recordNewInstance( o, 0 );

        Assertions.assertEquals( expected, o );
        Assertions.assertEquals( HeapStateUtils.size(), OffHeap.getAllocator().size() );
        Assertions.assertEquals( HeapStateUtils.top(), OffHeap.getAllocator().usedMemory() );
    }

    @Test
    @Order(2)
    void renewInstance() {
        Assumptions.assumeTrue( OffHeap.getAllocator().size() > 0 );

        //Constructor
        OffHeapFourLongs o2 = new OffHeapFourLongs(0L, 1L, 2L, 3L);
        HeapStateUtils.recordNewInstance( o2, 1 );
        //Reconstructor
        OffHeapFourLongs o1 = new OffHeapFourLongs( HeapStateUtils.offset(0) );

        Assertions.assertEquals( o2, o1 );
        Assertions.assertNotEquals( o2.getOffset(), o1.getOffset() );
        Assertions.assertFalse( o2 == o1 );

        Assertions.assertEquals( HeapStateUtils.size(), OffHeap.getAllocator().size() );
        Assertions.assertEquals( HeapStateUtils.top(), OffHeap.getAllocator().usedMemory() );
    }

    @Test
    @Order(3)
    void deleteInstance() {
        Assumptions.assumeTrue( OffHeap.getAllocator().size() > 0 );

        OffHeapFourLongs o1 = new OffHeapFourLongs( HeapStateUtils.offset(0) );
        OffHeapFourLongs o2 = new OffHeapFourLongs( HeapStateUtils.offset(1) );

        HeapStateUtils.recordDeletedInstance( o1, 0 );
        HeapStateUtils.recordDeletedInstance( o2, 1 );

        OffHeap.deleteInstance( o1 );
        OffHeap.deleteInstance( o2 );

        Assertions.assertEquals( HeapStateUtils.size(), OffHeap.getAllocator().size() );
        Assertions.assertEquals( HeapStateUtils.top(), OffHeap.getAllocator().usedMemory() );
    }

    @Test
    @Order(4)
    void multiNew() {
        Assumptions.assumeTrue( OffHeap.getAllocator().size() == 0 );

        for(int i=0; i < MULTI_PUT_LENGTH; i++) {
            OffHeapFourLongs o = new OffHeapFourLongs( i+0L, i+1L, i+2L, i+3L );
            HeapStateUtils.recordNewInstance( o, i );
        }

        Assertions.assertEquals( HeapStateUtils.size(), OffHeap.getAllocator().size() );
        Assertions.assertEquals( HeapStateUtils.top(), OffHeap.getAllocator().usedMemory() );
    }

    @Test
    @Order(5)
    void multiRecoverAndDelete() {
        Assumptions.assumeTrue( OffHeap.getAllocator().size() > 0 );

        for(int i=0; i < MULTI_PUT_LENGTH; i++) {
            OffHeapFourLongs o = new OffHeapFourLongs( HeapStateUtils.offset(i) );

            Assertions.assertEquals( i+0L, o.getL1() );
            Assertions.assertEquals( i+1L, o.getL2() );
            Assertions.assertEquals( i+2L, o.getL3() );
            Assertions.assertEquals( i+3L, o.getL4() );

            HeapStateUtils.recordDeletedInstance( o, i );
            OffHeap.deleteInstance( o );
        }

        Assertions.assertEquals( 0, OffHeap.getAllocator().size() );
        Assertions.assertEquals( MULTI_PUT_LENGTH * MemoryBlockHandle.size(), OffHeap.getAllocator().usedMemory() );
    }

    @AfterEach
    void tearDown() {
        //TODO Close HEAP
    }

    @AfterAll
    static void tearDownAll() {
        //TODO Close HEAP Factory
    }

}
