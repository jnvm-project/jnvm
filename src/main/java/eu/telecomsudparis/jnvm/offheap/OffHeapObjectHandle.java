package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public abstract class OffHeapObjectHandle {

    private transient long offset = -1L;
    private transient long base = -1L;

    //Constructor
    public OffHeapObjectHandle() {
        OffHeap.newInstance( this );
    }

    //Reconstructor
    public OffHeapObjectHandle(long offset) {
        OffHeap.recInstance( this, offset );
    }

    //Field accessors
    protected long getOffset() {
        return this.offset;
    }

    //Helpers
    protected MemoryBlockHandle block() {
        return OffHeap.getAllocator().blockFromOffset( this.offset );
    }

    protected long addressFromFieldOffset(long fieldOffset) {
        return base + fieldOffset;
    }

    //Instance methods
    protected void attach(long offset) {
        this.offset = offset;
        this.base = block().base();
    }

    protected void detach() {
        this.offset = -1L;
        this.base = -1L;
    }

    public abstract long size();

    //Java.lang.Object overrides
    @Override
    public int hashCode() {
        return Long.hashCode( this.offset );
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof OffHeapObjectHandle ) {
            return offset == ((OffHeapObjectHandle)obj).getOffset();
        }
        return false;
    }

    //Data manipulation methods
    protected void setLongField(long fieldOffset, long value) {
        unsafe.putLong( addressFromFieldOffset( fieldOffset ), value );
    }

    protected long getLongField(long fieldOffset) {
        return unsafe.getLong( addressFromFieldOffset( fieldOffset ) );
    }

    //Unsafe mechanics
    private static final sun.misc.Unsafe unsafe;
    static {
        unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();
    }

}
