package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public interface OffHeapObject {

    long getOffset();
    void attach(long offset);
    void detach();
    void destroy();
    void validate();
    void invalidate();
    void flush();
    default void fence() { unsafe.pfence(); }
    long classId();
    boolean mark();
    void descend();

    long addressFromFieldOffsetRO(long fieldOffset);
    long addressFromFieldOffsetRW(long fieldOffset);

    //Data manipulation methods
    default void setByteField(long fieldOffset, byte value) {
        unsafe.putByte( addressFromFieldOffsetRW( fieldOffset ), value );
    }

    default byte getByteField(long fieldOffset) {
        return unsafe.getByte( addressFromFieldOffsetRO( fieldOffset ) );
    }

    default void setCharField(long fieldOffset, char value) {
        unsafe.putChar( addressFromFieldOffsetRW( fieldOffset ), value );
    }

    default char getCharField(long fieldOffset) {
        return unsafe.getChar( addressFromFieldOffsetRO( fieldOffset ) );
    }

    default void setLongField(long fieldOffset, long value) {
        unsafe.putLong( addressFromFieldOffsetRW( fieldOffset ), value );
    }

    default long getAndAddLongField(long fieldOffset, long delta) {
        return unsafe.getAndAddLong( null, addressFromFieldOffsetRW( fieldOffset ), delta );
    }

    default long getLongField(long fieldOffset) {
        return unsafe.getLong( addressFromFieldOffsetRO( fieldOffset ) );
    }

    default int getIntegerField(long fieldOffset) {
        return unsafe.getInt( addressFromFieldOffsetRO( fieldOffset ) );
    }

    default void setIntegerField(long fieldOffset, int value) {
        unsafe.putInt( addressFromFieldOffsetRW( fieldOffset ), value );
    }

    default double getDoubleField(long fieldOffset) {
        return unsafe.getDouble( addressFromFieldOffsetRO( fieldOffset ) );
    }

    default void setDoubleField(long fieldOffset, double value) {
        unsafe.putDouble( addressFromFieldOffsetRW( fieldOffset ), value );
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

    default OffHeapObject unique() {
        OffHeap.instances.put( getOffset(), this );
        return this;
    }

    //Unsafe mechanics
    sun.misc.Unsafe unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();

}
