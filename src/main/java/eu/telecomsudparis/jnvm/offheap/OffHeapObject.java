package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public interface OffHeapObject {

    long getOffset();
    void attach(long offset);
    void detach();
    void destroy();
    long classId();

    long addressFromFieldOffset(long fieldOffset);

    //Data manipulation methods
    default void setByteField(long fieldOffset, byte value) {
        unsafe.putByte( addressFromFieldOffset( fieldOffset ), value );
    }

    default byte getByteField(long fieldOffset) {
        return unsafe.getByte( addressFromFieldOffset( fieldOffset ) );
    }

    default void setCharField(long fieldOffset, char value) {
        unsafe.putChar( addressFromFieldOffset( fieldOffset ), value );
    }

    default char getCharField(long fieldOffset) {
        return unsafe.getChar( addressFromFieldOffset( fieldOffset ) );
    }

    default void setLongField(long fieldOffset, long value) {
        unsafe.putLong( addressFromFieldOffset( fieldOffset ), value );
    }

    default long getAndAddLongField(long fieldOffset, long delta) {
        return unsafe.getAndAddLong( null, addressFromFieldOffset( fieldOffset ), delta );
    }

    default long getLongField(long fieldOffset) {
        return unsafe.getLong( addressFromFieldOffset( fieldOffset ) );
    }

    default int getIntegerField(long fieldOffset) {
        return unsafe.getInt( addressFromFieldOffset( fieldOffset ) );
    }

    default void setIntegerField(long fieldOffset, int value) {
        unsafe.putInt( addressFromFieldOffset( fieldOffset ), value );
    }

    default double getDoubleField(long fieldOffset) {
        return unsafe.getDouble( addressFromFieldOffset( fieldOffset ) );
    }

    default void setDoubleField(long fieldOffset, double value) {
        unsafe.putDouble( addressFromFieldOffset( fieldOffset ), value );
    }

    default void setHandleField(long fieldOffset, OffHeapObject ohoh) {
        setLongField( fieldOffset, ohoh.getOffset() - OffHeap.baseAddr() );
    }

    default OffHeapObject getHandleField(long fieldOffset) {
        return OffHeap.instanceFromOffset( OffHeap.baseAddr() + getLongField( fieldOffset ) );
    }

    default String getStringField(long fieldOffset) {
        return getHandleField( fieldOffset ).toString();
    }

    default void setStringField(long fieldOffset, String str) {
        setHandleField( fieldOffset, new OffHeapString(str) );
    }

    //Unsafe mechanics
    sun.misc.Unsafe unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();

}
