package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public abstract class OffHeapObjectHandle implements OffHeapObject {

    private transient long offset = -1L;
    private transient long base = -1L;
    private transient long faBase = -1L;
    private transient boolean recordable;

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
        return ( OffHeap.recording && recordable )
            ? addressFromFieldOffsetFARO( fieldOffset )
            : base + fieldOffset;
    }

    public long addressFromFieldOffsetRW(long fieldOffset) {
        return ( OffHeap.recording && recordable )
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
        this.recordable = block().isRecordable();
        block().setKlass( classId() );
    }

    public void detach() {
        //block().setRecordable( false );
        this.recordable = false;
        this.offset = -1L;
        this.base = -1L;
        this.faBase = -1L;
    }

    public void validate() {
        if( OffHeap.recording ) {
            //block().setRecordable( false );
            this.recordable = false;
            OffHeap.getLog().logValidate( this );
        } else {
            this.recordable = true;
            block().setRecordable( true );
            block().commit();
        }
    }

    public void invalidate() {
        //block().setRecordable( false );
        //this.recordable = false;
        if( OffHeap.recording && recordable ) {
            OffHeap.getLog().logInvalidate( this );
        } else {
            this.destroy();
        }
    }

    public void flush() {
        unsafe.writebackMemory( base - 8, size() + 8 );
    }

    public abstract long size();

    public boolean mark() {
        //System.out.println(this);
        return OffHeap.gcMark( this.offset );
    }
    public abstract void descend();

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

    protected static final sun.misc.Unsafe unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();

}
