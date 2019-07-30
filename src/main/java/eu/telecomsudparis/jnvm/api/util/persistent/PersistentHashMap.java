package eu.telecomsudparis.jnvm.api.util.persistent;

import java.io.Serializable;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.AbstractMap;

import eu.telecomsudparis.jnvm.api.PMemPool;
import net.bramp.unsafe.UnsafeHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PersistentHashMap<K,V> extends AbstractMap<K,V>
    implements PersistentMap<K,V> {

    private static final int DEFAULT_SIZE = 16;

    private transient HashMap<K,Integer> index;
    private transient HashMap<K,V> cache;

    private transient PMemPool pmemPool;

    public PersistentHashMap(PMemPool pmemPool) {
        this( DEFAULT_SIZE, pmemPool );
    }

    public PersistentHashMap(Map<? extends K, ? extends V> m, PMemPool pmemPool) {
        this( m.size(), pmemPool );
        putAll( m );
    }

    public PersistentHashMap(int initialCapacity, PMemPool pmemPool) {
        //TODO Connect to Persistent Pool
        this.pmemPool = pmemPool;
        //TODO Recover Persistent Pool
        if(!pmemPool.isLoaded())
            pmemPool.open();
        //TODO Get correct new Map sizes (sum)
        //int capacity = initialCapacity + pmemPool.getSize();
        int capacity = initialCapacity;

        if(index == null)
            index = new HashMap<K,Integer>( capacity );
        if(cache == null)
            cache = new HashMap<K,V>( capacity );

        //TODO Populate transient Maps
        //TODO Nicer way of iterating over PMemPools
        for( int idx=0; idx < pmemPool.getSize() - 1; idx += 2 ) {
            try {
                /*
                long addr = pmemPool.getAddress( idx );
                K k = (K) unsafe.allocateInstance( Object.class );
                V v = (V) unsafe.allocateInstance( Object.class );
                unsafe.copyMemory(null, addr, k, keyBaseOffset, keySize);
                unsafe.copyMemory(null, addr + keySize, v, valueBaseOffset, valueSize);
                */

                byte[] kbytes = new byte[ (int) keySize ];
                byte[] vbytes = new byte[ (int) valueSize ];
                pmemPool.get( kbytes, idx );
                pmemPool.get( vbytes, idx + 1 );
                K k = (K) toObject( kbytes );
                V v = (V) toObject( vbytes );
                index.put( k, idx );
                cache.put( k, v );
            } catch (Exception e) {
            }
        }
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
        return cache.containsValue( value );
    }

    public Set<K> keySet() {
        return index.keySet();
    }

    public Collection<V> values() {
        return cache.values();
    }

    public V put(K key, V value) {
        //Unsafe serialization
        /*
        byte[] kbytes = new byte[ (int) keySize ];
        byte[] vbytes = new byte[ (int) valueSize ];
        unsafe.copyMemory(key, keyBaseOffset, kbytes, arrayBaseOffset, keySize);
        unsafe.copyMemory(value, valueBaseOffset, vbytes, arrayBaseOffset, valueSize);
        */
        try {
            byte[] kbytes = toByteArray(key);
            byte[] vbytes = toByteArray(value);

            //TODO Send value to Persistent Pool
            //TODO Retrieve persistent address
            int idx = pmemPool.put( kbytes );
            pmemPool.put( vbytes );
            //TODO Put index in index Map
            index.put( key, idx );
        } catch (Exception e) {
        }
        return cache.put( key, value );
    }

    public V get(Object key) {
        V v = cache.get( key );
        if( v == null ) {
            Integer idx = index.get( key );
            if( idx == null ) {
                //TODO Search for value in Persistent Pool
                //idx = pmemPool.search( key );
                //TODO Put idx in index
                //index.put( key, idx );
                throw new IllegalStateException( "Not properly initialized" );
            }
            //TODO Retrieve value from addr in Persistent Pool
            //v = pmemPool.get( addr ).getValue();
            try {
                /*
                long addr = pmemPool.getAddress( idx );
                v = (V) unsafe.allocateInstance( Object.class );
                unsafe.copyMemory(null, addr + keySize, v, valueBaseOffset, valueSize);
                */
                byte[] vbytes = new byte[ (int) valueSize ];
                pmemPool.get( vbytes, idx + 1 );
                v = (V) toObject( vbytes );

                //TODO Put value in cache
                cache.put( (K) key, v );

                //For now, best we can do is returning null
                //return null;
            } catch (Exception e) {
            }
        }
        return v;
    }

    public V remove(Object key) {
        V v = get( key );

        //TODO Remove value in Persistent Pool
        pmemPool.remove( index.get( key ) );

        cache.remove( key );
        index.remove( key );

        return v;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        //TODO Reserve multiple slots in Persistent Pool for atomicity
        //TODO Put into specific slots

        //For now, just do a regular put
        for( Map.Entry<? extends K, ? extends V> e : m.entrySet() ) {
            put( e.getKey(), e.getValue() );
        }
    }

    public void clear() {
        //TODO Mark Persistent Pool empty
        pmemPool.clear();

        index.clear();
        cache.clear();
    }

    /* AbstractMap methods */

    public Set<Map.Entry<K,V>> entrySet() {
        return cache.entrySet();
    }

    /* ConcurrentMap methods */
    /*
     * For now, let's not worry about making this Map thread-safe,
     * since we should be able to open multiple PersistentMap from the same
     * Persistent Pool
     *
     */

    // Unsafe Mechanics
    private static final sun.misc.Unsafe unsafe;
    private static final long arrayBaseOffset;
    private static final long keyBaseOffset = 12L;
    private static final long keySize = 256L;
    private static final long valueBaseOffset = 12L;
    private static final long valueSize = 256L;
    static {
        try {
            unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();
            arrayBaseOffset = unsafe.arrayBaseOffset( byte[].class );
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    //Serialization
    public static byte[] toByteArray(Object obj) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
        return bytes;
    }

    public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return obj;
    }
}
