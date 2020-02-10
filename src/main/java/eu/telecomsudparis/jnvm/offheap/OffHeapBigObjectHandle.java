package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public abstract class OffHeapBigObjectHandle implements OffHeapObject {

    private static final long BYTES_PER_BASE = MemoryBlockHandle.size() - 8 - 8;
    private transient long offset = -1L;
    private transient long[] bases = null;

    //Constructor
    public OffHeapBigObjectHandle(long size) {
        long nblocks = size / BYTES_PER_BASE + 1;
        this.bases = new long[ (int) nblocks ];
        OffHeap.newInstance( this, size + nblocks * 8 );
    }

    //Reconstructor
    protected OffHeapBigObjectHandle() { }
    public static <K extends OffHeapBigObjectHandle> K rec(K ohboh, long offset) {
        return OffHeap.recInstance( ohboh, offset );
    }

    //Destructor
    public void destroy() {
        OffHeap.deleteInstance( this );
    }

    //Field accessors
    public long getOffset() {
        return this.offset;
    }

    public long[] getBases() {
        return this.bases;
    }

    public long addressFromFieldOffset(long fieldOffset) {
        return bases[ (int) ( fieldOffset / BYTES_PER_BASE ) ] + fieldOffset % BYTES_PER_BASE + 8;
    }

    //Instance methods
    public void attach(long offset) {
        long size = this.baseOffset() + unsafe.getLong( offset + 16 ) * this.indexScale();
        long nblocks = size / BYTES_PER_BASE + 1;
        this.bases = new long[ (int) nblocks ];
        this.offset = offset;
        long off = offset;
        for(int i=0; i<bases.length; i++) {
            bases[i] = OffHeap.getAllocator().blockFromOffset( off ).base();
            off = unsafe.getLong( bases[i] + 0 );
        }
    }

    public void attach(long[] offset) {
        this.offset = offset[0];
        for(int i=0; i<bases.length; i++) {
            bases[i] = OffHeap.getAllocator().blockFromOffset( offset[i] ).base();
            if( i+1 < offset.length )
                unsafe.putLong( bases[i] + 0, offset[i+1] );
            else
                unsafe.putLong( bases[i] + 0, -1 );
        }
    }

    public void detach() {
        this.offset = -1L;
        this.bases = null;
    }

    public abstract long size();
    public abstract long indexScale();
    public abstract long baseOffset();

    //Java.lang.Object overrides
    @Override
    public int hashCode() {
        return Long.hashCode( getOffset() );
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof OffHeapObject ) {
            return getOffset() == ((OffHeapObject) obj).getOffset();
        }
        return false;
    }

}
