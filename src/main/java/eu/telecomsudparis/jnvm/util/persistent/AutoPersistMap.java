package eu.telecomsudparis.jnvm.util.persistent;

import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.AbstractMap;
import java.util.Queue;
import java.util.ArrayDeque;

import eu.telecomsudparis.jnvm.offheap.MemoryBlockHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;
import eu.telecomsudparis.jnvm.offheap.OffHeap;


public class AutoPersistMap<K extends OffHeapObject,
                            V extends OffHeapObject,
                            I extends Convertible<K>,
                            J extends Convertible<V>>
        extends AbstractMap<K,V>
        implements Convertible<RecoverableMap<K, V>>, RecoverableMap<K,V> {

    private static final long CLASS_ID = OffHeap.Klass.register( AutoPersistMap.class );

    private static final ThreadLocal<Queue<Convertible>> workQueue =
        new ThreadLocal<Queue<Convertible>>() {
            @Override protected Queue<Convertible> initialValue() {
                return new ArrayDeque(30);
            }
    };
    private static final ThreadLocal<Queue<Convertible>> ptrQueue =
        new ThreadLocal<Queue<Convertible>>() {
            @Override protected Queue<Convertible> initialValue() {
                return new ArrayDeque(30);
            }
    };

    private RecoverableMap<K,V> map = null;
    private transient Map<I, J> vmap = null;

    //Constructor
    private AutoPersistMap(int initialSize) {
        this.vmap = null;
        this.map = new RecoverableStrongHashMap( initialSize );
        //OffHeap.Instances.put(map.getOffset(), this);
        OffHeap.getAllocator().blockFromOffset( map.getOffset() ).setKlass( CLASS_ID );
    }

    public AutoPersistMap(Map<I, J> m) {
        this.vmap = m;
        //this.map = new RecoverableStrongHashMap( m.size() );
        //OffHeap.getAllocator().blockFromOffset( map.getOffset() ).setKlass( CLASS_ID );
    }

    //Reconstructor
    public AutoPersistMap(long offset) {
        this.map = new RecoverableStrongHashMap( offset );
        //OffHeap.instances.put(map.getOffset(), this);
    }
    public AutoPersistMap(MemoryBlockHandle block) {
        this( block.getOffset() );
    }
    public static AutoPersistMap recover(String name, int initialSize) {
        OffHeapObject oho = OffHeap.rootInstances.get( name );
        AutoPersistMap ret = null;
        if( oho != null ) {
          if( oho instanceof AutoPersistMap ) {
            ret = (AutoPersistMap) oho;
          } else {
            throw new IllegalStateException("Root name already exists");
          }
        } else {
          ret = new AutoPersistMap( initialSize );
          OffHeap.rootInstances.put( new OffHeapString( name ), ret );
          ret.validate();
        }
        return ret;
    }

    /* Convertor */

    public RecoverableMap<K,V> copyToNVM() {
        if( map != null ) {
            return map;
        } else {
            map = new RecoverableStrongHashMap( vmap.size() );
            OffHeap.getAllocator().blockFromOffset( map.getOffset() ).setKlass( CLASS_ID );
            for(Map.Entry<I,J> entry : vmap.entrySet()) {
                K k = (K) makeObjectRecoverable( entry.getKey() );
                V v = (V) makeObjectRecoverable( entry.getValue() );
                map.put( k, v );
            }
            map.flush();
            map.validate();
            vmap = null;
            return this;
        }
    }

    /* AutoPersist Map methods */

    public V putConvert(I key, J value) {
        V oldValue = null;
        if( map != null ) {
            V v = (V) makeObjectRecoverable( value );
            if( map.containsKey( key ) ) {
                oldValue = map.replaceValue( key, v );
            } else {
                K k = (K) makeObjectRecoverable( key );
                oldValue = map.put( k, v );
            }
            if( oldValue != null ) {
                oldValue.invalidate();
            }
        } else {
            vmap.put( key, value );
        }
        unsafe.psync();
        return oldValue;
    }

    private OffHeapObject makeObjectRecoverable(Convertible object) {
        OffHeapObject ret = object.copyToNVM();
        //ConvertObjects
/*
        for(Convertible o : workQueue) {
            o.copyToNVM();
        }
*/
        return ret;
    }
    private static void addToQueueIfNotConverted(Convertible object) {
        workQueue.get().add( object );
    }

    /* Map methods */

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey( key );
    }

    public boolean containsValue(Object value) {
        return map.containsValue( value );
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public Collection<V> values() {
        //return map.clone();
        return null;
    }

    public V put(K key, V value) {
        return map.put( key, value );
    }

    public V replaceValue(Object key, V value) {
        return map.replaceValue( key, value );
    }

    public V get(Object key) {
        return map.get( key );
    }

    public V remove(Object key) {
        V v = map.remove( key );
        if( v != null ) {
            ((K) key).destroy();
        }
        return v;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for( Map.Entry<? extends K, ? extends V> e : m.entrySet() ) {
            put( e.getKey(), e.getValue() );
        }
    }

    public void clear() {
        map.clear();
    }

    /* AbstractMap methods */

    public Set<Map.Entry<K,V>> entrySet() {
        return map.entrySet();
    }

    /* ConcurrentMap methods */
    /*
     * For now, let's not worry about making this Map thread-safe,
     * since we should be able to open multiple PersistentMap from the same
     * Persistent Pool
     *
     */

    public long getOffset() { return map.getOffset(); }
    public void attach(long offset) { map.attach( offset ); }
    public void detach() { map.detach(); }
    public long classId() { return CLASS_ID; }
    public long addressFromFieldOffsetRO(long fieldOffset) {
        return map.addressFromFieldOffsetRO( fieldOffset );
    }
    public long addressFromFieldOffsetRW(long fieldOffset) {
        return map.addressFromFieldOffsetRW( fieldOffset );
    }
    public void validate() { map.validate(); }
    public void invalidate() { map.invalidate(); }
    public void destroy() { map.destroy(); }
    public void flush() { map.flush(); }
    public boolean mark() { return map.mark(); }
    public void descend() { map.descend(); }

    // Unsafe Mechanics
    private static final sun.misc.Unsafe unsafe;
    static {
        unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();
    }

}
