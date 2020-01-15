package eu.telecomsudparis.jnvm.util.persistent;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.AbstractMap;

import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapArray;


public class RecoverableHashMap<K extends OffHeapObject, V extends OffHeapObject>
        extends AbstractMap<K,V> implements PersistentMap<K,V>, OffHeapObject {

    private static final int DEFAULT_SIZE = 16;

    private transient HashMap<K,Long> index;
    private OffHeapArray<OffHeapNode<K,V>> table;

    static class OffHeapNode<K extends OffHeapObject, V extends OffHeapObject>
            extends OffHeapObjectHandle implements Map.Entry<K,V> {
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

        public long size() { return SIZE; }

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
	}

    //Reconstructor
    public RecoverableHashMap(long offset) {
        table = (OffHeapArray<OffHeapNode<K,V>>)OffHeapArray.rec( offset );
        long length = table.length();
        index = new HashMap<>( (int) length );

        for( long i=0; i<length; i++ ) {
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

    public V replace(Object key, V value) {
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
        if( (idx = index.get( key )) == null ) {
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
    public long length() { return table.length(); }
    public long addressFromFieldOffset(long fieldOffset) {
        return table.addressFromFieldOffset( fieldOffset );
    }
    public void destroy() { table.destroy(); }

}
