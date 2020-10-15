package eu.telecomsudparis.jnvm.offheap;

import java.util.Properties;
import java.util.HashMap;
import java.util.ArrayList;

import eu.telecomsudparis.jnvm.config.Environment;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableHashMap;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableStrongHashMap;

import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.lang.reflect.Constructor;


public class OffHeap {

    private static final Properties properties = Environment.getProperties();

    private transient static final MemoryPool pool;
    private transient static final MemoryAllocator allocator;
    public transient static final Map<Long, OffHeapObject> instances;

    //TODO Have a proper metablock layout declaration
    private static final long ROOT_INSTANCES = 16;
    public static final RecoverableHashMap<OffHeapString, OffHeapObject> rootInstances;

    //TODO Generate this from all classes extending OffHeapObject
    //     and the one existing on the memory pool metablock
    public enum Klass {
        A(0, OffHeapArray.class),
        B(1, OffHeapCharArray.class),
        C(2, OffHeapByteArray.class),
        D(3, OffHeapString.class),
        E(4, RecoverableHashMap.class),
        F(5, RecoverableHashMap.OffHeapNode.class),
        G(6, RecoverableStrongHashMap.class),
        H(7, RecoverableStrongHashMap.OffHeapNode.class);

        private static final Map<Class<?>, Long> BY_NAME = new HashMap<>();
        private static final Map<Long, Class<?>> BY_ID = new HashMap<>();

        static {
            for( OffHeap.Klass ohc : values()){
                BY_NAME.put( ohc.klazz, ohc.classId );
                BY_ID.put( ohc.classId, ohc.klazz );
            }
        }

        public final long classId;
        public final Class<?> klazz;

        private Klass(long classId, Class<?> klazz) {
            this.classId = classId;
            this.klazz = klazz;
        }

        public static Class<?> klazz(long classId) {
            return BY_ID.get( classId );
        }

        public static long register(Class<?> klass) {
            return BY_NAME.get( klass );
        }

        public static long registerUserKlass(Class<?> klass, long id) {
            Long klassId = BY_NAME.get( klass );
            if( klassId == null ) {
                klassId = id;
                BY_NAME.put( klass, klassId );
                BY_ID.put( klassId, klass );
            }
            return klassId;
        }
    }

    static {
        //TODO Factory design where pools are known from cfg file and lazily
        //     loaded as Heap instance is requested to Heap factory.
        String path = properties.getProperty(Environment.JNVM_HEAP_PATH);
        long size = Long.parseLong( properties.getProperty(Environment.JNVM_HEAP_SIZE) );

        instances = new WeakHashMap<>();
        pool = MemoryPool.open( path, size );
        allocator = MemoryAllocator.recover( pool.address(), pool.limit() );
        //TODO Store OffHeap state, including offsets to our objects, in a metablock.
        if( allocator.top() == 0 ) {
            rootInstances = new RecoverableHashMap(10);
        } else {
            rootInstances = new RecoverableHashMap( baseAddr() + ROOT_INSTANCES );
        }
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

    public static MemoryAllocator getAllocator() { return allocator; }
    public static long baseAddr() { return pool.address(); }

    //Constructor
    public static <K extends OffHeapObject> K newInstance(K k) {
        k.attach( allocator.allocateBlock().getOffset() );
        //instances.put( k.getOffset(), k );
        return k;
    }

    public static <K extends OffHeapBigObjectHandle> K newInstance(K k, long size) {
        k.attach( allocator.allocateMemory( size, k.getBases() ) );
        //instances.put( k.getOffset(), k );
        return k;
    }

    //Reconstructor
    public static <K extends OffHeapObject> K recInstance(K k, long offset) {
        k.attach( allocator.blockFromOffset( offset ).getOffset() );
        //instances.put( k.getOffset(), k );
        return k;
    }

    public static <K extends OffHeapObject> K newInstance(MemoryBlockHandle block) {
        K k = null;
        try {
            Class klass = Klass.klazz( block.getKlass() );
            Constructor kons = klass.getConstructor( MemoryBlockHandle.class );
            k = (K) kons.newInstance( block );
            k.attach( block.getOffset() );
            //instances.put( k.getOffset(), k );
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
        //instances.remove( k.getOffset() );
    }

    public static <K extends OffHeapBigObjectHandle> void deleteInstance(K k) {
        allocator.freeMemory( k.getOffset(), k.size() );
        k.detach();
        //instances.remove( k.getOffset() );
    }

    public static <K extends OffHeapObjectHandle> void deleteInstance(K k) {
        allocator.freeMemory( k.getOffset(), k.size() );
        k.detach();
        //instances.remove( k.getOffset() );
    }

}
