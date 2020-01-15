package eu.telecomsudparis.jnvm.offheap;

import java.util.Properties;
import java.util.HashMap;
import java.util.ArrayList;

import eu.telecomsudparis.jnvm.config.Environment;


public class OffHeap {

    private static final Properties properties = Environment.getProperties();

    private transient static final MemoryPool pool;
    private transient static final MemoryAllocator allocator;
    public transient static final HashMap<Long, OffHeapObject> instances;
    private transient static final ArrayList<String> classes;

    static {
        //TODO Factory design where pools are known from cfg file and lazily
        //     loaded as Heap instance is requested to Heap factory.
        String path = properties.getProperty("jnvm.heap.path");
        long size = Long.parseLong( properties.getProperty("jnvm.heap.size") );

        pool = MemoryPool.open( path, size );
        allocator = MemoryAllocator.recover( pool.address(), pool.limit() );
        instances = new HashMap<>();
        //TODO Store OffHeap state, including offsets to our objects, in a metablock.
        classes = new ArrayList<>();
        //Eager object pointer mapping initialization
        //TODO iterate over MemoryPool and fill instances hash table
        /*
        pool.stream.filter( MemoryAllocator::recoverBlock )
                   .filter( OffHeap::isUserClass )
                   .forEach( OffHeap::newInstance )
        */
    }

    private OffHeap() {
        throw new UnsupportedOperationException();
    }

    //TODO implement classId to class pointer retrieval mechanism
    private static <K extends OffHeapObject> Class<?> klazz(long classId) {
        try {
            return Class.forName( classes.get( (int) classId ) );
        } catch(Exception e) {
        }
        return null;
    }
    public static <K extends OffHeapObject> void registerClass(Class<K> klass) {
        classes.add( klass.getName() );
    }
    public static MemoryAllocator getAllocator() { return allocator; }

    //Constructor
    public static <K extends OffHeapObject> K newInstance(K k) {
        k.attach( allocator.allocateBlock().getOffset() );
        instances.put( k.getOffset(), k );
        return k;
    }

    public static <K extends OffHeapBigObjectHandle> K newInstance(K k, long size) {
        k.attach( allocator.allocateMemory( size, k.getBases() ) );
        instances.put( k.getOffset(), k );
        return k;
    }

    //Reconstructor
    public static <K extends OffHeapObject> K recInstance(K k, long offset) {
        k.attach( allocator.blockFromOffset( offset ).getOffset() );
        instances.put( k.getOffset(), k );
        return k;
    }

    public static <K extends OffHeapObject> K newInstance(MemoryBlockHandle block) {
        K k = null;
        try {
            k = (K) klazz( block.getKlass() ).newInstance();
            k.attach( block.getOffset() );
            instances.put( k.getOffset(), k );
        } catch(Exception e) {
        }
        return k;
    }

    //TODO find a way to store ordinary object pointer in the off-heap
    public static <K extends OffHeapObject> K instanceFromOffset(long offset) {
        K k = null;
        if( ( k = (K) instances.get( offset ) ) == null ) {
            //Lazy object pointer mapping initialization
            k = newInstance( allocator.blockFromOffset( offset ) );
        }
        return k;
    }

    public static <K extends OffHeapObject> void deleteInstance(K k, long size) {
        allocator.freeMemory( k.getOffset(), size );
        k.detach();
        instances.remove( k.getOffset() );
    }

    public static <K extends OffHeapBigObjectHandle> void deleteInstance(K k) {
        allocator.freeMemory( k.getOffset(), k.size() );
        k.detach();
        instances.remove( k.getOffset() );
    }

    public static <K extends OffHeapObjectHandle> void deleteInstance(K k) {
        allocator.freeMemory( k.getOffset(), k.size() );
        k.detach();
        instances.remove( k.getOffset() );
    }

}
