package eu.telecomsudparis.jnvm.offheap;

import java.util.Properties;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.BitSet;

import eu.telecomsudparis.jnvm.config.Environment;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableMap;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableHashMap;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableStrongHashMap;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableStrongTreeMap;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableStrongSkipListMap;
import eu.telecomsudparis.jnvm.util.persistent.AutoPersistMap;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.WeakHashMap;
import java.lang.reflect.Constructor;


public class OffHeap {

    private static final Properties properties = Environment.getProperties();

    private transient static final MemoryPool pool;
    private transient static final MemoryAllocator allocator;
    private transient static final BitSet marks;
    public transient static final Map<Long, OffHeapObject> instances;

    //TODO Have a proper metablock layout declaration
    private static final long METABLOCK = 16;
    private static final Metablock metablock;
    public static final RecoverableMap<OffHeapString, OffHeapObject> rootInstances;
    private static final OffHeapArray<OffHeapString> userKlasses;
    private static final OffHeapRedoLog log;
    public static boolean recording = false;


    //TODO Generate this from all classes extending OffHeapObject
    //     and the one existing on the memory pool metablock
    public enum Klass {
        A(0, Metablock.class),
        B(1, OffHeapArray.class),
        C(2, OffHeapCharArray.class),
        D(3, OffHeapByteArray.class),
        E(4, OffHeapString.class),
        F(5, OffHeapCachedString.class),
        G(6, RecoverableHashMap.class),
        H(7, RecoverableHashMap.OffHeapNode.class),
        I(8, RecoverableStrongHashMap.class),
        J(9, RecoverableStrongHashMap.OffHeapNode.class),
        K(10, RecoverableStrongTreeMap.class),
        L(11, RecoverableStrongTreeMap.OffHeapNode.class),
        M(12, RecoverableStrongSkipListMap.class),
        N(13, RecoverableStrongSkipListMap.OffHeapNode.class),
        O(14, AutoPersistMap.class),
        P(15, OffHeapRedoLog.class),
        Q(16, OffHeapRedoLog.CopyEntry.class),
        R(17, OffHeapRedoLog.ValidateEntry.class),
        S(18, OffHeapRedoLog.InvalidateEntry.class);

        private static final Map<Class<?>, Long> BY_NAME = new ConcurrentHashMap<>();
        private static final Map<Long, Class<?>> BY_ID = new ConcurrentHashMap<>();

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

        private static long userKlassOffset(long userKlassId) {
            return userKlassId + Klass.values().length;
        }

        private static void registerUserKlass(Class<?> klass, long klassId) {
            BY_NAME.put( klass, klassId );
            BY_ID.put( klassId, klass );
        }

        public static long registerUserKlass(Class<?> klass) {
            Long klassId = BY_NAME.get( klass );
            if( klassId == null ) {
                OffHeapString ohs = new OffHeapString( klass.getName() );
                Long userKlassId = userKlasses.add( ohs );
                klassId = userKlassOffset( userKlassId );
                registerUserKlass( klass, klassId );
            }
            return klassId;
        }

        private static void loadUserKlasses(OffHeapArray<OffHeapString> userKlasses) {
            ClassLoader loader = OffHeap.class.getClassLoader();
            for( int i=0; i<userKlasses.length(); i++ ){
                OffHeapString ohs = userKlasses.get( i );
                Class<?> klazz = null;
                try {
                    klazz = loader.loadClass( ohs.toString() );
                } catch(Exception e) {
                    e.printStackTrace(System.out);
                }
                Long klassId = userKlassOffset( i );
                registerUserKlass( klazz, klassId );
            }
        }
    }

    private static class Metablock extends OffHeapObjectHandle {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeap.Metablock.class );

        private static final long[] offsets = { 0, 8, 16 };
        private static final long SIZE = 24;

        Metablock() { super(); }
        Metablock(long offset) { super( offset ); }
        Metablock setRoot(RecoverableMap root) { setHandleField( offsets[0], root ); return this; }
        Metablock setLog(OffHeapRedoLog log) { setHandleField( offsets[1], log ); return this; }
        Metablock setUserKlasses(OffHeapArray userK) { setHandleField( offsets[2], userK ); return this; }
        RecoverableMap getRoot() { return (RecoverableMap) getHandleField( offsets[0] ); }
        OffHeapRedoLog getLog() { return (OffHeapRedoLog) getHandleField( offsets[1] ); }
        OffHeapArray getUserKlasses() { return (OffHeapArray) getHandleField( offsets[2] ); }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }
        public void descend() {
            //No-op;
        }

    }

    static {
        //TODO Factory design where pools are known from cfg file and lazily
        //     loaded as Heap instance is requested to Heap factory.
        String path = properties.getProperty(Environment.JNVM_HEAP_PATH);
        long size = Long.parseLong( properties.getProperty(Environment.JNVM_HEAP_SIZE) );

        instances = new WeakHashMap<>();
        marks = new BitSet( (int)(size / MemoryBlockHandle.size()) );
        // Bootstrap
        //  1. open pool
        //  2. init allocator
        //  3. reconstruct log (apply redo)
        //  4. reconstruct root (gc -> fix dangling)
        //  5. reconstruct allocator free list
        pool = MemoryPool.open( path, size );
        allocator = new MemoryAllocator( pool.address(), pool.limit() );
        //allocator = MemoryAllocator.recover( pool.address(), pool.limit() );
        //TODO Store OffHeap state, including offsets to our objects, in a metablock.
        if( allocator.top() == 0 ) {
            metablock = new Metablock();
            log = new OffHeapRedoLog( 100 );
            rootInstances = new RecoverableStrongHashMap( 10 );
            userKlasses = new OffHeapArray( 10 );
            metablock.setRoot( rootInstances )
                     .setUserKlasses( userKlasses )
                     .setLog( log );
        } else {
            metablock = new Metablock( baseAddr() + METABLOCK );
            log = metablock.getLog();
            userKlasses = metablock.getUserKlasses();
            OffHeap.Klass.loadUserKlasses( userKlasses );
            rootInstances = metablock.getRoot();
        }
        //Eager object pointer mapping initialization
        //TODO iterate over MemoryPool and fill instances hash table
        /*
        pool.stream.filter( MemoryAllocator::recoverBlock )
                   .filter( OffHeap::isUserClass )
                   .forEach( OffHeap::newInstance )
        */
        log.redo();
        gcStartMarking();
        MemoryAllocator.recover( allocator );
    }

    private OffHeap() {
        throw new UnsupportedOperationException();
    }

    public static final MemoryAllocator getAllocator() { return allocator; }
    public static final OffHeapRedoLog getLog() { return log; }
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
            e.printStackTrace(System.out);
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

    public static void startRecording() {
        log.clear();
        recording = true;
    }

    public static void stopRecording() {
        recording = false;
        log.redo();
    }

    public static boolean gcMark(long offset) {
        int idx = (int)(( offset - baseAddr() ) / MemoryBlockHandle.size() );
        boolean marked = marks.get( idx );
        if( !marked ) marks.set( idx );
        return marked;
    }

    public static void gcMarkNoCheck(long offset) {
        int idx = (int)(( offset - baseAddr() ) / MemoryBlockHandle.size() );
        marks.set( idx );
        //System.out.println(marks.cardinality());
    }

    public static void gcStartMarking() {
        System.out.println("Starting marking");
        long start = System.nanoTime();
        if( !metablock.mark() )
            metablock.descend();
        if( !log.mark() )
            log.descend();
        if( !userKlasses.mark() )
            userKlasses.descend();
        if( !rootInstances.mark() )
            rootInstances.descend();
        long end = System.nanoTime();
        System.out.println("Marking finished in (nsec): " + (end - start) );
    }

}
