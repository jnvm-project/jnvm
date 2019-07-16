package eu.telecomsudparis.jnvm.api.util.persistent;

import java.io.Serializable;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.AbstractMap;

public class PersistentHashMap<K,V> extends AbstractMap<K,V>
    implements PersistentMap<K,V> {

    private static final int DEFAULT_SIZE = 16;

    private transient HashMap<K,Long> index;
    private transient HashMap<K,V> cache;

    public PersistentHashMap() {
        this( DEFAULT_SIZE );
    }

    public PersistentHashMap(Map<? extends K, ? extends V> m) {
        this( m.size() );
        putAll( m );
    }

    public PersistentHashMap(int initialCapacity) {
        //TODO Connect to Persistent Pool
        //TODO Recover Persistent Pool
        //TODO Get correct new Map sizes (sum)

        if(index == null)
            index = new HashMap<K,Long>( initialCapacity );
        if(cache == null)
            cache = new HashMap<K,V>( initialCapacity );

        //TODO Populate transient Maps
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
        //TODO Send value to Persistent Pool
        //TODO Retrieve persistent address
        //TODO Put address in index Map

        return cache.put( key, value );
    }

    public V get(Object key) {
        V v = cache.get( key );
        if( v == null ) {
            Long addr = index.get( key );
            if( addr == null ) {
                //TODO Search for value in Persistent Pool
                //TODO Put addr in index
            }
            //TODO Retrieve value from addr in Persistent Pool
            //TODO Put value in cache

            //For now, best we can do is returning null
            return null;
        }
        return v;
    }

    public V remove(Object key) {
        V v = get( key );
        cache.remove( key );
        index.remove( key );

        //TODO Remove value in Persistent Pool

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
}
