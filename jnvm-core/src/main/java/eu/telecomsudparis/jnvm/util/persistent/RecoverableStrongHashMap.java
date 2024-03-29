package eu.telecomsudparis.jnvm.util.persistent;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.LongStream;
import java.util.concurrent.atomic.AtomicReference;

import eu.telecomsudparis.jnvm.offheap.MemoryBlockHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapArray;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;
import eu.telecomsudparis.jnvm.offheap.OffHeap;


public class RecoverableStrongHashMap<K extends OffHeapObject, V extends OffHeapObject>
        extends AbstractMap<K,V> implements RecoverableMap<K,V> {

    private static final long CLASS_ID = OffHeap.Klass.register( RecoverableStrongHashMap.class );

    private static final int DEFAULT_SIZE = 16;

    private transient Queue<Long> reclaimed;
    private transient Map<K,OffHeapNode<K,V>> index;
    private OffHeapArray<OffHeapNode<K,V>> table;

    public static class OffHeapNode<K extends OffHeapObject, V extends OffHeapObject>
            extends OffHeapObjectHandle implements Map.Entry<K,V> {
        private static final long CLASS_ID = OffHeap.Klass.register( RecoverableStrongHashMap.OffHeapNode.class );
        final static long[] offsets = { 0, 8 };
        final static long SIZE = 16;

        private final transient K key;
        private transient V value;
        private long tableIndex = -1;
        //private transient AtomicReference<V> value;

        //Constructor
        OffHeapNode(K key, V val) {
            super();
            setHandleField( offsets[0], key );
            setHandleField( offsets[1], val );
            this.key = key;
            this.value = val;
            //this.value = new AtomicReference<>( val );
        }

        //Reconstructor
        OffHeapNode(long offset) {
            super( null, offset );
            this.key = (K) getHandleField( offsets[0] );
            this.value = null;
            //this.value = (V) getHandleField( offsets[1] );
            //this.value = new AtomicReference();
            //this.value = new AtomicReference( (V) getHandleField( offsets[1] ) );
        }
        public OffHeapNode(MemoryBlockHandle block) {
            this( block.getOffset() );
        }
        public OffHeapNode(Void v, long offset) {
            this( offset );
        }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }
        public void descend() {
            K key = getKey();
            if( !key.mark() )
                key.descend();
            V value = getValue();
            if( !value.mark() )
                value.descend();
        }

        public final K getKey() { return this.key; }
        public final V getValue() {
            if( this.value == null ) {
                this.value = (V) getHandleField( offsets[1] );
            }
            return this.value;
            /*
            V val;
            if( (val = this.value.get()) == null ) {
                val = (V) getHandleField( offsets[1] );
                this.value.lazySet( val );
            }
            return val;
            */
            //return this.value.get();

        }
        public final V setValue(V newValue) {
            V oldValue = getValue();
            //V oldValue = this.value;
            setHandleField( offsets[1], newValue );
            this.value = newValue;
            return oldValue;
            /*
            V oldVal;
            do {
                oldVal = this.value.get();
                setHandleField( offsets[1], newValue );
            } while( !this.value.compareAndSet( oldVal, newValue ) );
            return oldVal;
            */
        }
    }

    //Constructor
    public RecoverableStrongHashMap() {
        this( DEFAULT_SIZE );
    }

    public RecoverableStrongHashMap(Map<? extends K, ? extends V> m) {
        this( m.size() );
        putAll( m );
    }

    public RecoverableStrongHashMap(int initialSize) {
        index = new ConcurrentHashMap<>( initialSize );
        table = new OffHeapArray<>( initialSize );
        reclaimed = new ConcurrentLinkedDeque<>();
        //OffHeap.Instances.put(table.getOffset(), this);
        OffHeap.getAllocator().blockFromOffset( table.getOffset() ).setKlass( CLASS_ID );
    }

    //Reconstructor
    public RecoverableStrongHashMap(long offset) {
        table = (OffHeapArray<OffHeapNode<K,V>>)OffHeapArray.rec( offset );
        this.reclaimed = new ConcurrentLinkedDeque<>();
        //OffHeap.instances.put(table.getOffset(), this);
//        long length = table.length();
//        index = new ConcurrentHashMap<>( (int) length );

/*
        final int threadCount = ( length < 10 ) ? 1 : 10;
        final int idxPerThread = ((int) length) / threadCount ;

        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>(threadCount);
        for(int i=0; i<threadCount; i++) {
            final int tid = i;
            Callable c = new Callable() {
                @Override
                public Void call() throws Exception {
                    for( long k=tid*idxPerThread; k<(tid+1)*idxPerThread; k++ ) {
                        OffHeapNode<K,V> entry = table.get( k );
                        index.put( entry.getKey(), entry );
                    }
                    return null;
                }
            };
            tasks.add(c);
        }
        try {
        List<Future<Void>> results = exec.invokeAll(tasks);
        for( Future<Void> result : results ) {
            result.get();
        }
        } catch( Exception e ) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
        exec.shutdown();
*/
/*
        final int threadCount = ( length < 10 ) ? 1 : 10;
        final int idxPerThread = ((int) length) / threadCount ;

        //LongStream.range(0, threadCount).forEach( i -> {
        LongStream.range(0, threadCount).parallel().forEach( i -> {
            for( long k=i*idxPerThread; k<(i+1)*idxPerThread; k++ ) {
              OffHeapNode<K,V> entry = table.get( k );
              index.put( entry.getKey(), entry );
            }
        } );
*/
/*
        LongStream.range(0, length).parallel().forEach( i -> {
            OffHeapNode<K,V> entry = table.get( i );
            index.put( entry.getKey(), entry );
        } );
*/

        //System.out.println(table.length());
        //System.out.println(this.size());

        //for( long i=0; i<length; i++ ) {
            /*
            OffHeapNode<K,V> entry = null; K entryKey = null;
                entry = table.get( i );
                if( entry == null ) {
                    System.out.println("No key found at index: " + String.valueOf(i) );
                    System.out.println("No key found at offset: " + String.valueOf( entry.addressFromFieldOffset( entry.offsets[0] ) ) );
                    break;
                }
                entryKey = entry.getKey();
                if( entryKey == null ) {
                    System.out.println("No table entry at index: " + String.valueOf(i) );
                    break;
                }
            */
            //OffHeapNode<K,V> entry = table.get( i );
            //K entryKey = entry.getKey();
            //V entryValue = entry.getValue();
            //index.put( entryKey, i );
       //     OffHeapNode<K,V> ohnode = table.get( i );
       //     index.put( ohnode.getKey(), ohnode );
       // }
        /*
        table.forEach( entry -> index.put( entry.getKey(), entry.getValue() ) );
        */
    }
    private void checkIndexNonNull() {
        if( index == null ) {
            long length = table.length();
            index = new ConcurrentHashMap<>( (int) length );
            final int threadCount = ( length < 10 ) ? 1 : 10;
            final int idxPerThread = ((int) length) / threadCount ;
            LongStream.range(0, threadCount).parallel().forEach( i -> {
                for( long k=i*idxPerThread; k<(i+1)*idxPerThread; k++ ) {
                  OffHeapNode<K,V> entry = table.getOrDefault( k, null );
                  if( entry != null ) {
                      index.put( entry.getKey(), entry );
                  } else {
                      reclaimed.add( k );
                  }
                }
            } );
        }
    }
    public RecoverableStrongHashMap(MemoryBlockHandle block) {
        this( block.getOffset() );
    }
    public RecoverableStrongHashMap(Void v, long offset) {
        this( offset );
    }
    public static RecoverableStrongHashMap recover(String name, int initialSize) {
        OffHeapObject oho = OffHeap.rootInstances.get( name );
        RecoverableStrongHashMap ret = null;
        if( oho != null ) {
          if( oho instanceof RecoverableStrongHashMap ) {
            ret = (RecoverableStrongHashMap) oho;
          } else {
            throw new IllegalStateException("Root name already exists");
          }
        } else {
          ret = new RecoverableStrongHashMap( initialSize );
          OffHeap.rootInstances.put( new OffHeapString( name ), ret );
          ret.validate();
        }
        return ret;
    }

    /* Map methods */

    public int size() {
        checkIndexNonNull();
        return index.size();
    }

    public boolean isEmpty() {
        checkIndexNonNull();
        return index.isEmpty();
    }

    public boolean containsKey(Object key) {
        checkIndexNonNull();
        return index.containsKey( key );
    }

    public boolean containsValue(Object value) {
        return table.contains( value );
    }

    public Set<K> keySet() {
        checkIndexNonNull();
        return index.keySet();
    }

    public Collection<V> values() {
        //return table.clone();
        return null;
    }

    public V put(K key, V value) {
        checkIndexNonNull();
        OffHeapNode<K,V> entry = null; V oldValue = null;
        if( (entry = index.get( key )) == null ) {
            entry = new OffHeapNode<>( key, value );
            Long tableIndex = reclaimed.poll();
            if( tableIndex == null ) {
                entry.tableIndex = table.add( entry );
            } else {
                table.set( tableIndex, entry );
            }
            index.put( key, entry );
            entry.validate();
        } else {
            oldValue = entry.setValue( value );
        }
        return oldValue;
    }

    public V replaceValue(Object key, V value) {
        checkIndexNonNull();
        OffHeapNode<K,V> entry = null; V oldValue = null;
        if( ( entry = index.get( key ) ) != null ) {
            oldValue = entry.setValue( value );
        }
        return oldValue;
    }

    public V get(Object key) {
        checkIndexNonNull();
        OffHeapNode<K,V> entry = null;
        if( ( entry = index.get( key ) ) != null ) {
            return entry.getValue();
        }
        return null;
    }

    public V remove(Object key) {
        checkIndexNonNull();
        OffHeapNode<K,V> entry = null; V oldValue = null;
        long tableIndex;
        if( (entry = index.get( key )) != null ) {
            oldValue = entry.getValue();
            tableIndex = entry.tableIndex;
            index.remove( key );
            entry.invalidate();
            table.clear( tableIndex );
            reclaimed.add( tableIndex );
        }
        return oldValue;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        //For now, just do a regular put
        for( Map.Entry<? extends K, ? extends V> e : m.entrySet() ) {
            put( e.getKey(), e.getValue() );
        }
    }

    public void clear() {
        checkIndexNonNull();
        table.clear();
        index.clear();
    }

    /* AbstractMap methods */

    public Set<Map.Entry<K,V>> entrySet() {
        return table.clone().asSet();
    }

    /* ConcurrentMap methods */
    /*
     * For now, let's not worry about making this Map thread-safe,
     * since we should be able to open multiple PersistentMap from the same
     * Persistent Pool
     *
     */

    // Unsafe Mechanics
    /*
    private static final sun.misc.Unsafe unsafe;
    private static final long arrayBaseOffset;
    private static long keyBaseOffset = 12L;
    private static long keySize = 256L;
    private static long valueBaseOffset = 12L;
    private static long valueSize = 256L;
    static {
        try {
            unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();
            arrayBaseOffset = unsafe.arrayBaseOffset( byte[].class );
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    */

    public long getOffset() { return table.getOffset(); }
    public void attach(long offset) { table.attach( offset ); }
    public void detach() { table.detach(); }
    public long classId() { return CLASS_ID; }
    public long length() { return table.length(); }
    public long addressFromFieldOffsetRO(long fieldOffset) {
        return table.addressFromFieldOffsetRO( fieldOffset );
    }
    public long addressFromFieldOffsetRW(long fieldOffset) {
        return table.addressFromFieldOffsetRW( fieldOffset );
    }
    public void validate() { table.validate(); }
    public void invalidate() { table.invalidate(); }
    public void resetFa() { table.resetFa(); }
    public void destroy() { table.destroy(); }
    public void flush() { table.flush(); }
    public boolean mark() { return table.mark(); }
    public void descend() {
        //Do not descend through the table, iterating over the index is way faster
        //table.descend();
        //If index is absent, rebuild it while descending
        if( index == null ) {
            long length = table.length();
            index = new ConcurrentHashMap<>( (int) length );
            final int threadCount = ( length < 10 ) ? 1 : 10;
            final int idxPerThread = ((int) length) / threadCount ;
            LongStream.range(0, threadCount).parallel().forEach( i -> {
                for( long k=i*idxPerThread; k<(i+1)*idxPerThread; k++ ) {
                    OffHeapNode<K,V> node = table.getOrDefault( k, null );
                    if( node != null ) {
                        if( !node.mark() ) {
                            node.descend();
                        }
                        index.put( node.getKey(), node );
                    } else {
                        reclaimed.add( k );
                    }
                }
            } );
        } else if( index.size() <= 20 ) {
            for( OffHeapNode<K,V> node : index.values() ) {
                if( !node.mark() ) {
                    node.descend();
                }
            }
        } else {
            //index.values().stream().filter( node -> !node.mark() ).forEach( node -> node.descend() );
            index.values().parallelStream().filter( node -> !node.mark() ).forEach( node -> node.descend() );
        }
    }

}
