package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public abstract class OffHeapObjectHandle implements OffHeapObject {

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
    public long getOffset() {
        return this.offset;
    }

    //Helpers
    protected MemoryBlockHandle block() {
        return OffHeap.getAllocator().blockFromOffset( this.offset );
    }

    public long addressFromFieldOffset(long fieldOffset) {
        return base + fieldOffset;
    }

    //Instance methods
    public void attach(long offset) {
        this.offset = offset;
        this.base = block().base();
    }

    public void detach() {
        this.offset = -1L;
        this.base = -1L;
    }

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

}
