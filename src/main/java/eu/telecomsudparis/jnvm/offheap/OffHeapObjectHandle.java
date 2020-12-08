package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public abstract class OffHeapObjectHandle implements OffHeapObject {

    private transient long offset = -1L;
    private transient long base = -1L;
    private transient long faBase = -1L;
    //private transient boolean recordable = false;

    //Constructor
    public OffHeapObjectHandle() {
        OffHeap.newInstance( this );
    }

    //Reconstructor
    public OffHeapObjectHandle(long offset) {
        OffHeap.recInstance( this, offset );
    }

    //Destructor
    public void destroy() {
        OffHeap.deleteInstance( this );
    }

    //Field accessors
    public long getOffset() {
        return this.offset;
    }

    //Helpers
    protected MemoryBlockHandle block() {
        return OffHeap.getAllocator().blockFromOffset( this.offset );
    }

    public long addressFromFieldOffsetRO(long fieldOffset) {
        return ( OffHeap.recording && block().isRecordable() )
            ? addressFromFieldOffsetFARO( fieldOffset )
            : base + fieldOffset;
    }

    public long addressFromFieldOffsetRW(long fieldOffset) {
        return ( OffHeap.recording && block().isRecordable() )
            ? addressFromFieldOffsetFARW( fieldOffset )
            : base + fieldOffset;
    }

    public long addressFromFieldOffsetFARO(long fieldOffset) {
        return ( faBase != -1L )
            ? faBase + fieldOffset
            : base + fieldOffset;
    }

    public long addressFromFieldOffsetFARW(long fieldOffset) {
        long b;
        if( faBase != -1L ) { b = faBase; }
        else {
            MemoryBlockHandle block = OffHeap.getAllocator().allocateBlock();
            MemoryBlockHandle.copy( block.getOffset(), this.offset );
            faBase = block.base();
            OffHeap.getLog().logCopy( this.offset, block.getOffset() );
            b = faBase;
        }
        return b + fieldOffset;
    }

    //Instance methods
    public void attach(long offset) {
        this.offset = offset;
        this.base = block().base();
        //this.recordable = block().isValid();
        block().setKlass( classId() );
    }

    public void detach() {
        this.offset = -1L;
        this.base = -1L;
        this.faBase = -1L;
        block().setRecordable( false );
    }

    public void validate() {
        if( OffHeap.recording ) {
            block().setRecordable( false );
            OffHeap.getLog().logValidate( this.offset );
        } else {
            block().setRecordable( true );
            block().commit();
        }
    }

    public void invalidate() {
        block().setRecordable( false );
        if( OffHeap.recording ) {
            OffHeap.getLog().logInvalidate( this.offset );
        } else {
            this.destroy();
        }
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

}
