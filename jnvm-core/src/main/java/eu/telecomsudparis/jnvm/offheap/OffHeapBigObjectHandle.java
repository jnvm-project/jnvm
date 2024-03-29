package eu.telecomsudparis.jnvm.offheap;

import java.util.Arrays;
import java.util.ArrayList;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public abstract class OffHeapBigObjectHandle implements OffHeapObject {

    public static final long BYTES_PER_BASE = MemoryBlockHandle.size() - 8 - 8;
    private transient long offset = -1L;
    private transient long[] bases = null;
    private transient long[] faBases = null;
    private transient boolean recordable = false;
    private transient ArrayList<Integer> touchedFaBases = new ArrayList<>();
    //TODO Evaluate whether recordable can be deducted from validity bit

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
    public OffHeapBigObjectHandle(Void nill, long offset) {
        rec( this, offset );
    }

    //Destructor
    public void destroy() {
        //Not very clean, but more reliable to pass the « bases » directly
        //down to the allocator for proper deletion.
        OffHeap.deleteInstance( this );
        //Deletion with computed size should be OK, but double check
        //this beforehand.
        /*
        long nblocks = size() / BYTES_PER_BASE + 1;
        OffHeap.deleteInstance( this, size() + nblocks * 8 );
        //OffHeap.deleteInstance( this, size() + bases.length * 8 );
        */
    }

    //Field accessors
    public long getOffset() {
        return this.offset;
    }

    public long[] getBases() {
        return this.bases;
    }

    protected MemoryBlockHandle block() {
        return OffHeap.getAllocator().blockFromOffset( this.offset );
    }

    public long addressFromFieldOffsetRO(long fieldOffset) {
        return ( OffHeap.recording && recordable )
            ? addressFromFieldOffsetFARO( fieldOffset )
            : bases[ (int) ( fieldOffset / BYTES_PER_BASE ) ] + fieldOffset % BYTES_PER_BASE + 8;
    }

    public long addressFromFieldOffsetRW(long fieldOffset) {
        return ( OffHeap.recording && recordable )
            ? addressFromFieldOffsetFARW( fieldOffset )
            : bases[ (int) ( fieldOffset / BYTES_PER_BASE ) ] + fieldOffset % BYTES_PER_BASE + 8;
    }

    private long addressFromFieldOffsetFARO(long fieldOffset) {
        long b;
        if( faBases != null && (b=faBases[ (int) ( fieldOffset / BYTES_PER_BASE ) ]) != 0 )
            return b + fieldOffset % BYTES_PER_BASE + 8;
        else
            return bases[ (int) ( fieldOffset / BYTES_PER_BASE ) ] + fieldOffset % BYTES_PER_BASE + 8;
    }

    private long addressFromFieldOffsetFARW(long fieldOffset) {
        long b;
        if( faBases == null ) {
            faBases = new long[ bases.length ];
        }
        if( (b=faBases[ (int) ( fieldOffset / BYTES_PER_BASE ) ]) == 0 ) {
            MemoryBlockHandle block = OffHeap.getAllocator().allocateBlock();
            long old = this.bases[ (int) ( fieldOffset / BYTES_PER_BASE ) ] - 8;
            MemoryBlockHandle.copy( block.getOffset(), old );
            faBases[(int) ( fieldOffset / BYTES_PER_BASE )] = block.base();
            OffHeap.getLog().logCopy( old, block.getOffset() );
            b = block.base();
            this.touchedFaBases.add( (int) ( fieldOffset / BYTES_PER_BASE ) );
            if( touchedFaBases.size() == 1 )
                OffHeap.getLog().touch( this );
        }
        return b + fieldOffset % BYTES_PER_BASE + 8;
    }

    //Instance methods
    public void attach(long offset) {
        long size = this.baseOffset() + unsafe.getLong( offset + 16 ) * this.indexScale();
        long nblocks = size / BYTES_PER_BASE + 1;
        this.bases = new long[ (int) nblocks ];
        this.offset = offset;
        this.recordable = block().isRecordable();
        long off = offset;
        for(int i=0; i<bases.length; i++) {
            MemoryBlockHandle block = OffHeap.getAllocator().blockFromOffset( off );
            bases[i] = block.base();
            off = OffHeap.baseAddr() + unsafe.getLong( bases[i] + 0 );
        }
    }

    public void attach(long[] offset) {
        /* Debug print, delete this
        if( offset.length != bases.length )
            System.out.println(bases.length + " " + offset.length);
        */
        this.offset = offset[0];
        //Fresh allocation, can't be recordable at this stage
        //this.recordable = block().isRecordable();
        for(int i=0; i<bases.length; i++) {
            MemoryBlockHandle block = OffHeap.getAllocator().blockFromOffset( offset[i] );
            bases[i] = block.base();
            block.setKlass( classId() );
            block.setMultiBlock( i != 0 );
            if( i+1 < offset.length )
                unsafe.putLong( bases[i] + 0, offset[i+1] - OffHeap.baseAddr() );
            else
                unsafe.putLong( bases[i] + 0, -1 );
        }
    }

    public void detach() {
        //block().setRecordable( false );
        this.recordable = false;
        this.offset = -1L;
        this.bases = null;
        this.faBases = null;
        this.touchedFaBases.clear();
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
        //Flush object blocks finely
        final long blockSize = MemoryBlockHandle.size();
        long dataSize = size();
        int i=0;
        while( dataSize > BYTES_PER_BASE ) {
            unsafe.writebackMemory( bases[i] - 8, blockSize );
            i++;
            dataSize -= BYTES_PER_BASE;
        }
        //Only flush used bytes in last block
        //unsafe.writebackMemory( bases[i] - 8, dataSize );
        //Flush last block whole
        unsafe.writebackMemory( bases[i] - 8, blockSize );
        /*
        //Flush object blocks whole
        //Bad implementation, would flush unused blocks/slots in arrays
        for(int i=0; i<bases.length; i++) {
            MemoryBlockHandle block = OffHeap.getAllocator().blockFromOffset( bases[i] - 8 );
            block.flush();
        }
        */
    }

    public void resetFa() {
        //Duplicated blocks are freed when clearing the log
        touchedFaBases.forEach( base -> { faBases[base] = 0; } );
        touchedFaBases.clear();
    }

    public abstract long size();
    public abstract long indexScale();
    public abstract long baseOffset();

    public boolean mark() {
        boolean set = OffHeap.gcMark( bases[0] - 8 );
        if( !set ) {
            if( bases.length < 10 ) {
            for( int i=1; i<bases.length; i++ ) {
                OffHeap.gcMarkNoCheck( bases[i] - 8 );
            }
            } else {
                Arrays.stream(bases).parallel().map( i -> i-8 ).forEach( OffHeap::gcMarkNoCheck );
            }
        }
        return set;
    }
    //Unused
    public static boolean mark(long offset) {
        boolean set = OffHeap.gcMark( offset );
        if( !set ) {
            long off = unsafe.getLong( offset + 8 );
            while( off != -1 ) {
                off += OffHeap.baseAddr();
                OffHeap.gcMarkNoCheck( off );
                off = unsafe.getLong( off + 8 );
            }
        }
        return set;
    }
    public abstract void descend();

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

    protected static final sun.misc.Unsafe unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();

}
