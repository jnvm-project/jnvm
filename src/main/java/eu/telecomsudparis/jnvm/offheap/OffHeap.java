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
    private static final long METABLOCK = 16;
    private static final Metablock metablock;
    public static final RecoverableHashMap<OffHeapString, OffHeapObject> rootInstances;
    private static final OffHeapRedoLog log;


    //TODO Generate this from all classes extending OffHeapObject
    //     and the one existing on the memory pool metablock
    public enum Klass {
        A(0, Metablock.class),
        B(1, OffHeapArray.class),
        C(2, OffHeapCharArray.class),
        D(3, OffHeapByteArray.class),
        E(4, OffHeapString.class),
        F(5, RecoverableHashMap.class),
        G(6, RecoverableHashMap.OffHeapNode.class),
        H(7, RecoverableStrongHashMap.class),
        I(8, RecoverableStrongHashMap.OffHeapNode.class),
        J(9, OffHeapRedoLog.class),
        K(10, OffHeapRedoLog.CopyEntry.class),
        L(11, OffHeapRedoLog.ValidateEntry.class),
        M(12, OffHeapRedoLog.InvalidateEntry.class);

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

    private static class Metablock extends OffHeapObjectHandle {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeap.Metablock.class );

        private static final long[] offsets = { 0, 8 };
        private static final long SIZE = 16;

        Metablock() { super(); }
        Metablock(long offset) { super( offset ); }
        Metablock setRoot(RecoverableHashMap root) { setHandleField( offsets[0], root ); return this; }
        Metablock setLog(OffHeapRedoLog log) { setHandleField( offsets[1], log ); return this; }
        RecoverableHashMap getRoot() { return (RecoverableHashMap) getHandleField( offsets[0] ); }
        OffHeapRedoLog getLog() { return (OffHeapRedoLog) getHandleField( offsets[1] ); }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }

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
            metablock = new Metablock();
            log = new OffHeapRedoLog( 50 );
            rootInstances = new RecoverableHashMap(10);
            metablock.setRoot( rootInstances )
                     .setLog( log );
        } else {
            metablock = new Metablock( baseAddr() + METABLOCK );
            log = metablock.getLog();
            rootInstances = metablock.getRoot();
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
