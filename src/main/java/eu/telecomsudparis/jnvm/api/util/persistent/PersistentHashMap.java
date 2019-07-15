package eu.telecomsudparis.jnvm.api.util.persistent;

import java.io.Serializable;
import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentMap;

public class PersistentHashMap<K,V> extends AbstractMap<K,V>
    implements PersistentMap<K,V>, ConcurrentMap<K,V> {

    public PersistentHashMap() {
        //TODO Implement me !
    }

    public PersistentHashMap(int initialCapacity) {
        //TODO Implement me !
    }

    public PersistentHashMap(Map<? extends K, ? extends V> m) {
        //TODO Implement me !
    }

    /* Map methods */

    public int size() {
        //TODO Implement me !
        return 0;
    }

    public boolean isEmpty() {
        //TODO Implement me !
        return false;
    }

    public boolean containsKey(Object key) {
        //TODO Implement me !
        return false;
    }

    public boolean containsValue(Object value) {
        //TODO Implement me !
        return false;
    }

    public Set<K> keySet() {
        //TODO Implement me !
        return null;
    }

    public Collection<V> values() {
        //TODO Implement me !
        return null;
    }

    public V put(K key, V value) {
        //TODO Implement me !
        return null;
    }

    public V get(Object key) {
        //TODO Implement me !
        return null;
    }

    public V remove(Object key) {
        //TODO Implement me !
        return null;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        //TODO Implement me !
    }

    public void clear() {
        //TODO Implement me !
    }

    /* AbstractMap methods */

    public Set<Map.Entry<K,V>> entrySet() {
        //TODO Implement me !
        return null;
    }

    /* ConcurrentMap methods */

    public V putIfAbsent(K key, V value) {
        //TODO Implement me !
        return null;
    }

    public boolean remove(Object key, Object value) {
        //TODO Implement me !
        return false;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        //TODO Implement me !
        return false;
    }

    public V replace(K key, V value) {
        //TODO Implement me !
        return null;
    }

}
