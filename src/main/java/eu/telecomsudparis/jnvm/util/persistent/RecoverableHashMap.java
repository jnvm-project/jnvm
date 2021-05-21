package eu.telecomsudparis.jnvm.util.persistent;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
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

    private transient HashMap<K,Long> index;
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
            super( offset );
        }
        public OffHeapNode(MemoryBlockHandle block) {
            this( block.getOffset() );
        }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }

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
        index = new HashMap<>( initialSize );
        table = new OffHeapArray<>( initialSize );
        //OffHeap.instances.put(table.getOffset(), this);
        OffHeap.getAllocator().blockFromOffset( table.getOffset() ).setKlass( CLASS_ID );
    }

    //Reconstructor
    public RecoverableHashMap(long offset) {
        table = (OffHeapArray<OffHeapNode<K,V>>)OffHeapArray.rec( offset );
        //OffHeap.instances.put(table.getOffset(), this);
        long length = table.length();
        index = new HashMap<>( (int) length );

        for( long i=0; i<length; i++ ) {
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
            index.put( table.get( i ).getKey(), i );
        }
        /*
        table.forEach( entry -> index.put( entry.getKey(), entry.getValue() ) );
        */
    }
    public RecoverableHashMap(MemoryBlockHandle block) {
        this( block.getOffset() );
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
        }
        return ret;
    }

    /* Map methods */

    public int size() {
        return index.size();
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    public boolean containsKey(Object key) {
        return index.containsKey( key );
    }

    public boolean containsValue(Object value) {
        return table.contains( value );
    }

    public Set<K> keySet() {
        return index.keySet();
    }

    public Collection<V> values() {
        //return table.clone();
        return null;
    }

    public V put(K key, V value) {
        Long idx; V oldValue = null;
        if( (idx = index.get( key )) == null ) {
            idx = table.add( new OffHeapNode<>( key, value ) );
            index.put( key, idx );
        } else {
            oldValue = table.get( idx ).setValue( value );
        }
        return oldValue;
    }

    public V replaceValue(Object key, V value) {
        Long idx; V oldValue = null;
        if( ( idx = index.get( key ) ) != null ) {
            oldValue = table.get( idx ).setValue( value );
        }
        return oldValue;
    }

    public V get(Object key) {
        Long idx;
        if( ( idx = index.get( key ) ) != null ) {
            return table.get( idx ).getValue();
        }
        return null;
    }

    public V remove(Object key) {
        Long idx; V oldValue = null;
        if( (idx = index.get( key )) != null ) {
            oldValue = table.remove( idx ).getValue();
            index.remove( key );
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
    public void destroy() { table.destroy(); }
    public void flush() { table.flush(); }

}
