package eu.telecomsudparis.jnvm.util.persistent;

import eu.telecomsudparis.jnvm.offheap.OffHeapObject;

public interface Convertible<V extends OffHeapObject> {
    V copyToNVM();
}
