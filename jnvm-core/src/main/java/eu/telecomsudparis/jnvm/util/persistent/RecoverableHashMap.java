package eu.telecomsudparis.jnvm.util.persistent;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.LongStream;
import java.util.Collection;
import java.util.AbstractMap;

import eu.telecomsudparis.jnvm.offheap.MemoryBlockHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapArray;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;
import eu.telecomsudparis.jnvm.offheap.OffHeap;


public class RecoverableHashMap<K extends OffHeapObject, V extends OffHeapObject>
        extends AbstractMap<K,V> implements RecoverableMap<K,V> {

    private static final long CLASS_ID = OffHeap.Klass.register( RecoverableHashMap.class );

    private static final int DEFAULT_SIZE = 16;

    private transient Map<K,Long> index;
    private OffHeapArray<OffHeapNode<K,V>> table;

    public static class OffHeapNode<K extends OffHeapObject, V extends OffHeapObject>
            extends OffHeapObjectHandle implements Map.Entry<K,V> {
        private static final long CLASS_ID = OffHeap.Klass.register( RecoverableHashMap.OffHeapNode.class );
        final static long[] offsets = { 0, 8 };
        final static long SIZE = 16;

        //Constructor
        OffHeapNode(K key, V value) {
            super();
            setHandleField( offsets[0], key );
            setHandleField( offsets[1], value );
        }

        //Reconstructor
        OffHeapNode(long offset) {
            super( null, offset );
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

        public final K getKey() { return (K) getHandleField( offsets[0] ); }
        public final V getValue() { return (V) getHandleField( offsets[1] ); }
        public final V setValue(V newValue) {
            V oldValue = (V) getHandleField( offsets[1] );
            setHandleField( offsets[1], newValue );
            return oldValue;
        }
    }

    //Constructor
    public RecoverableHashMap() {
        this( DEFAULT_SIZE );
    }

    public RecoverableHashMap(Map<? extends K, ? extends V> m) {
        this( m.size() );
        putAll( m );
    }

    public RecoverableHashMap(int initialSize) {
        //index = new HashMap<>( initialSize );
        index = new ConcurrentHashMap<>( initialSize );
        table = new OffHeapArray<>( initialSize );
        //OffHeap.instances.put(table.getOffset(), this);
        OffHeap.getAllocator().blockFromOffset( table.getOffset() ).setKlass( CLASS_ID );
    }

    //Reconstructor
    public RecoverableHashMap(long offset) {
        table = (OffHeapArray<OffHeapNode<K,V>>)OffHeapArray.rec( offset );
        //OffHeap.instances.put(table.getOffset(), this);
        //long length = table.length();
        //index = new HashMap<>( (int) length );
        /*
        index = new ConcurrentHashMap<>( (int) length );
        final int threadCount = ( length < 10 ) ? 1 : 10;
        final int idxPerThread = ((int) length) / threadCount ;
        LongStream.range(0, threadCount).parallel().forEach( i -> {
            for( long k=i*idxPerThread; k<(i+1)*idxPerThread; k++ ) {
              index.put( table.get( k ).getKey(), k );
            }
        } );
        */

//        for( long i=0; i<length; i++ ) {
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
//            index.put( table.get( i ).getKey(), i );
//        }
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
                  index.put( table.get( k ).getKey(), k );
                }
            } );
        }
    }
    public RecoverableHashMap(MemoryBlockHandle block) {
        this( block.getOffset() );
    }
    public RecoverableHashMap(Void v, long offset) {
        this( offset );
    }
    public static RecoverableHashMap recover(String name, int initialSize) {
        OffHeapObject oho = OffHeap.rootInstances.get( name );
        RecoverableHashMap ret = null;
        if( oho != null ) {
          if( oho instanceof RecoverableHashMap ) {
            ret = (RecoverableHashMap) oho;
          } else {
            throw new IllegalStateException("Root name already exists");
          }
        } else {
          ret = new RecoverableHashMap( initialSize );
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
        checkIndexNonNull();
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
        Long idx; V oldValue = null;
        if( (idx = index.get( key )) == null ) {
            OffHeapNode<K,V> entry = new OffHeapNode<>( key, value );
            idx = table.add( entry );
            index.put( key, idx );
            entry.validate();
        } else {
            oldValue = table.get( idx ).setValue( value );
        }
        return oldValue;
    }

    public V replaceValue(Object key, V value) {
        checkIndexNonNull();
        Long idx; V oldValue = null;
        if( ( idx = index.get( key ) ) != null ) {
            oldValue = table.get( idx ).setValue( value );
        }
        return oldValue;
    }

    public V get(Object key) {
        checkIndexNonNull();
        Long idx;
        if( ( idx = index.get( key ) ) != null ) {
            return table.get( idx ).getValue();
        }
        return null;
    }

    public V remove(Object key) {
        checkIndexNonNull();
        Long idx; V oldValue = null;
        if( (idx = index.get( key )) != null ) {
            OffHeapNode<K,V> entry = table.remove( idx );
            oldValue = entry.getValue();
            index.remove( key );
            entry.invalidate();
            table.clear( idx );
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
        if( index == null ) {
            long length = table.length();
            //index = new ConcurrentHashMap<>( (int) length );
            final int threadCount = ( length < 10 ) ? 1 : 10;
            final int idxPerThread = ((int) length) / threadCount ;
            LongStream.range(0, threadCount).parallel().forEach( i -> {
                for( long k=i*idxPerThread; k<(i+1)*idxPerThread; k++ ) {
                  OffHeapNode<K,V> node = table.get( k );
                  if( !node.mark() ) {
                    node.descend();
                  }
                  //index.put( node.getKey(), k );
                }
            } );
        } else if( index.size() <= 20 ) {
            table.forEach( node -> {
                if( !node.mark() ) {
                    node.descend();
                }
            });
        } else {
            //index.values().stream().filter( node -> !node.mark() ).forEach( node -> node.descend() );
            index.values().parallelStream()
                          .map( k -> table.get( k ) )
                          .filter( node -> !node.mark() )
                          .forEach( node -> node.descend() );
        }
    }

}
