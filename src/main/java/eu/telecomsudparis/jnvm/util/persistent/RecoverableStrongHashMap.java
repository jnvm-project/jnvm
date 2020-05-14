package eu.telecomsudparis.jnvm.util.persistent;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.AbstractMap;

import eu.telecomsudparis.jnvm.offheap.MemoryBlockHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapArray;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;
import eu.telecomsudparis.jnvm.offheap.OffHeap;


public class RecoverableStrongHashMap<K extends OffHeapObject, V extends OffHeapObject>
        extends AbstractMap<K,V> implements PersistentMap<K,V>, OffHeapObject {

    private static final long CLASS_ID = OffHeap.Klass.register( RecoverableStrongHashMap.class );

    private static final int DEFAULT_SIZE = 16;

    private transient HashMap<K,Long> index;
    private transient ArrayList<OffHeapNode<K,V>> cacheTable;
    private OffHeapArray<OffHeapNode<K,V>> table;

    public static class OffHeapNode<K extends OffHeapObject, V extends OffHeapObject>
            extends OffHeapObjectHandle implements Map.Entry<K,V> {
        private static final long CLASS_ID = OffHeap.Klass.register( RecoverableStrongHashMap.OffHeapNode.class );
        final static long[] offsets = { 0, 8 };
        final static long SIZE = 16;

        private final transient K key;
        private transient V value;

        //Constructor
        OffHeapNode(K key, V value) {
            super();
            setHandleField( offsets[0], key );
            setHandleField( offsets[1], value );
            this.key = key;
            this.value = value;
        }

        //Reconstructor
        OffHeapNode(long offset) {
            super( offset );
            this.key = (K) getHandleField( offsets[0] );
            this.value = (V) getHandleField( offsets[1] );
        }
        public OffHeapNode(MemoryBlockHandle block) {
            this( block.getOffset() );
        }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }

        public final K getKey() { return this.key; }
        public final V getValue() { return this.value; }
        public final V setValue(V newValue) {
            V oldValue = this.value;
            setHandleField( offsets[1], newValue );
            this.value = newValue;
            return oldValue;
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
        index = new HashMap<>( initialSize );
        table = new OffHeapArray<>( initialSize );
        cacheTable = new ArrayList<>( initialSize );
        //OffHeap.Instances.put(table.getOffset(), this);
        OffHeap.getAllocator().blockFromOffset( table.getOffset() ).setKlass( CLASS_ID );
    }

    //Reconstructor
    public RecoverableStrongHashMap(long offset) {
        table = (OffHeapArray<OffHeapNode<K,V>>)OffHeapArray.rec( offset );
        //OffHeap.instances.put(table.getOffset(), this);
        long length = table.length();
        index = new HashMap<>( (int) length );
        cacheTable = new ArrayList<>( (int) length );

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
            cacheTable.add( (int) i, table.get( i ) );
        }
        /*
        table.forEach( entry -> index.put( entry.getKey(), entry.getValue() ) );
        */
    }
    public RecoverableStrongHashMap(MemoryBlockHandle block) {
        this( block.getOffset() );
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
            OffHeapNode<K,V> entry = new OffHeapNode<>( key, value );
            idx = table.add( entry );
            cacheTable.add( idx.intValue(), entry );
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
            return cacheTable.get( idx.intValue() ).getValue();
        }
        return null;
    }

    public V remove(Object key) {
        Long idx; V oldValue = null;
        if( (idx = index.get( key )) != null ) {
            oldValue = table.remove( idx ).getValue();
            cacheTable.remove( idx );
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
        cacheTable.clear();
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
    public long addressFromFieldOffset(long fieldOffset) {
        return table.addressFromFieldOffset( fieldOffset );
    }
    public void destroy() { table.destroy(); }

}
