package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public interface OffHeapObject {

    long getOffset();
    void attach(long offset);
    void detach();
    void destroy();

    long addressFromFieldOffset(long fieldOffset);

    //Data manipulation methods
    default void setCharField(long fieldOffset, char value) {
        unsafe.putChar( addressFromFieldOffset( fieldOffset ), value );
    }

    default char getCharField(long fieldOffset) {
        return unsafe.getChar( addressFromFieldOffset( fieldOffset ) );
    }

    default void setLongField(long fieldOffset, long value) {
        unsafe.putLong( addressFromFieldOffset( fieldOffset ), value );
    }

    default long getLongField(long fieldOffset) {
        return unsafe.getLong( addressFromFieldOffset( fieldOffset ) );
    }

    default void setHandleField(long fieldOffset, OffHeapObject ohoh) {
        setLongField( fieldOffset, ohoh.getOffset() );
    }

    default OffHeapObject getHandleField(long fieldOffset) {
        return OffHeap.instanceFromOffset( getLongField( fieldOffset ) );
    }

    //Unsafe mechanics
    sun.misc.Unsafe unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();

}
