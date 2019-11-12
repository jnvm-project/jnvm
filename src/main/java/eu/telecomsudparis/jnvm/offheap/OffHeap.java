package eu.telecomsudparis.jnvm.offheap;

import java.util.Properties;

import eu.telecomsudparis.jnvm.config.Environment;


public class OffHeap {

    private static final Properties properties = Environment.getProperties();

    private transient static final MemoryPool pool;
    private transient static final MemoryAllocator allocator;

    static {
        //TODO Factory design where pools are known from cfg file and lazily
        //     loaded as Heap instance is requested to Heap factory.
        String path = properties.getProperty("jnvm.heap.path");
        long size = Long.parseLong( properties.getProperty("jnvm.heap.size") );

        pool = MemoryPool.open( path, size );
        allocator = MemoryAllocator.recover( pool.address(), pool.limit() );
    }

    private OffHeap() {
        throw new UnsupportedOperationException();
    }

    public static MemoryAllocator getAllocator() { return allocator; }

    //Constructor
    public static <K extends OffHeapObjectHandle> K newInstance(K k) {
        k.attach( allocator.allocateBlock().getOffset() );
        return k;
    }

    //Reconstructor
    public static <K extends OffHeapObjectHandle> K recInstance(K k, long offset) {
        k.attach( allocator.blockFromOffset( offset ).getOffset() );
        return k;
    }

    public static <K extends OffHeapObjectHandle> void deleteInstance(K k) {
        allocator.freeMemory( k.getOffset(), k.size() );
        k.detach();
    }

}
