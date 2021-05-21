package eu.telecomsudparis.jnvm.util.persistent;

import eu.telecomsudparis.jnvm.offheap.OffHeapObject;

public interface RecoverableMap<K,V> extends PersistentMap<K,V>, OffHeapObject {
    V replaceValue(Object key, V value);
}
