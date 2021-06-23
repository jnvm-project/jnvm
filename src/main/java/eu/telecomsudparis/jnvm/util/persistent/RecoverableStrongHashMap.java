package eu.telecomsudparis.jnvm.util.persistent;

import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.LongStream;

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

    private transient Map<K,OffHeapNode<K,V>> index;
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
            //this.value = null;
            this.value = (V) getHandleField( offsets[1] );
        }
        public OffHeapNode(MemoryBlockHandle block) {
            this( block.getOffset() );
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
/*
            if( this.value == null ) {
                this.value = (V) getHandleField( offsets[1] );
            }
*/
            return this.value;
        }
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
        index = new ConcurrentHashMap<>( initialSize );
        table = new OffHeapArray<>( initialSize );
        //OffHeap.Instances.put(table.getOffset(), this);
        OffHeap.getAllocator().blockFromOffset( table.getOffset() ).setKlass( CLASS_ID );
    }

    //Reconstructor
    public RecoverableStrongHashMap(long offset) {
        table = (OffHeapArray<OffHeapNode<K,V>>)OffHeapArray.rec( offset );
        //OffHeap.instances.put(table.getOffset(), this);
        long length = table.length();
        index = new ConcurrentHashMap<>( (int) length );

        final int threadCount = ( length < 10 ) ? 1 : 10;
        final int idxPerThread = ((int) length) / threadCount ;

        //LongStream.range(0, threadCount).forEach( i -> {
        LongStream.range(0, threadCount).parallel().forEach( i -> {
            for( long k=i*idxPerThread; k<(i+1)*idxPerThread; k++ ) {
              OffHeapNode<K,V> entry = table.get( k );
              index.put( entry.getKey(), entry );
              //System.out.println("titi");
            }
        } );

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
          ret.validate();
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
        OffHeapNode<K,V> entry = null; V oldValue = null;
        if( (entry = index.get( key )) == null ) {
            entry = new OffHeapNode<>( key, value );
            table.add( entry );
            index.put( key, entry );
            entry.validate();
        } else {
            oldValue = entry.setValue( value );
        }
        return oldValue;
    }

    public V replaceValue(Object key, V value) {
        OffHeapNode<K,V> entry = null; V oldValue = null;
        if( ( entry = index.get( key ) ) != null ) {
            oldValue = entry.setValue( value );
        }
        return oldValue;
    }

    public V get(Object key) {
        OffHeapNode<K,V> entry = null;
        if( ( entry = index.get( key ) ) != null ) {
            return entry.getValue();
        }
        return null;
    }

    public V remove(Object key) {
        OffHeapNode<K,V> entry = null; V oldValue = null;
        if( (entry = index.get( key )) != null ) {
            oldValue = entry.getValue();
            index.remove( key );
            //TODO: remove node from OffHeap table
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
    public boolean mark() { return table.mark(); }
    public void descend() { table.descend(); }

}
