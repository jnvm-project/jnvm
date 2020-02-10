package eu.telecomsudparis.jnvm.offheap;

import java.util.Properties;
import java.util.HashMap;
import java.util.ArrayList;

import eu.telecomsudparis.jnvm.config.Environment;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableHashMap;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Constructor;


public class OffHeap {

    private static final Properties properties = Environment.getProperties();

    private transient static final MemoryPool pool;
    private transient static final MemoryAllocator allocator;
    public transient static final HashMap<Long, OffHeapObject> instances;
    private transient static final ArrayList<String> classes;

    //TODO Generate this from all classes extending OffHeapObject
    //     and the one existing on the memory pool metablock
    public enum Klass {
        A(0, OffHeapArray.class),
        B(1, OffHeapCharArray.class),
        C(2, OffHeapByteArray.class),
        D(3, OffHeapString.class),
        E(4, RecoverableHashMap.class),
        F(5, RecoverableHashMap.OffHeapNode.class);

        private static final Map<Class<?>, Klass> BY_NAME = new HashMap<>();
        private static final Map<Long, Klass> BY_ID = new HashMap<>();

        static {
            for( OffHeap.Klass ohc : values()){
                BY_NAME.put( ohc.klazz, ohc );
                BY_ID.put( ohc.classId, ohc );
            }
        }

        public final long classId;
        public final Class<?> klazz;

        private Klass(long classId, Class<?> klazz) {
            this.classId = classId;
            this.klazz = klazz;
        }

        public static Class<?> klazz(long classId) {
            return BY_ID.get( classId ).klazz;
        }

        public static long register(Class<?> klass) {
            return BY_NAME.get( klass ).classId;
        }
    }

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
            Class klass = Klass.klazz( block.getKlass() );
            Constructor kons = klass.getConstructor( MemoryBlockHandle.class );
            k = (K) kons.newInstance( block );
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
