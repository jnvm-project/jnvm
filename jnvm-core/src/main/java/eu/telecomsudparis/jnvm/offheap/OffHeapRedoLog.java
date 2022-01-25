package eu.telecomsudparis.jnvm.offheap;

import java.util.ArrayList;

import eu.telecomsudparis.jnvm.offheap.MemoryBlockHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapArray;
import eu.telecomsudparis.jnvm.offheap.OffHeap;


public class OffHeapRedoLog implements OffHeapObject {
    private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.class );
    private static final int DEFAULT_SIZE = 16;

    private ArrayList<OffHeapObject> touched;
    private OffHeapArray<Entry> table;

    private interface Entry extends OffHeapObject {
        void apply();
    }

    public static class CopyEntry extends OffHeapObjectHandle implements Entry {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.CopyEntry.class );
        final static long[] offsets = { 0, 8 };
        final static long SIZE = 16;

        //Constructor
        CopyEntry(long orig, long copy) {
            super();
            setAddrField( offsets[0], orig );
            setAddrField( offsets[1], copy );
        }
        //Reconstructor
        public CopyEntry(MemoryBlockHandle block) { super( null, block.getOffset() ); }
        public CopyEntry(Void v, long offset) { super( null, offset ); }

        @Override
        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }
        public void descend() {
            //No-op;
        }
        public void destroy() {
            OffHeap.getAllocator().freeBlock( new MemoryBlockHandle( getNew() ));
            super.destroy();
        }

        public final long getOld() { return getAddrField( offsets[0] ); }
        public final long getNew() { return getAddrField( offsets[1] ); }
        public void apply() { MemoryBlockHandle.copy(getOld(), getNew()); }
    }

    public static class ValidateEntry extends OffHeapObjectHandle implements Entry {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.ValidateEntry.class );
        final static long[] offsets = { 0 };
        final static long SIZE = 8;

/*
        ValidateEntry(OffHeapObject oho) {
            super();
            setHandleField( offsets[0], oho );
        }
*/
        ValidateEntry(long block) {
            super();
            setAddrField( offsets[0], block );
        }
        public ValidateEntry(MemoryBlockHandle block) { super( null, block.getOffset() ); }
        public ValidateEntry(Void v, long offset) { super( null, offset ); }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }
        public void descend() {
            //No-op;
        }

        public final MemoryBlockHandle getBlock() {
            return OffHeap.getAllocator()
                          .blockFromOffset( getAddrField( offsets[0] ));
        }
        public void apply() { getBlock().commit(); getBlock().setRecordable( true ); }
//        public void apply() { getHandleField( offsets[0] ).validate(); }
    }

    public static class InvalidateEntry extends OffHeapObjectHandle implements Entry {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.InvalidateEntry.class );
        final static long[] offsets = { 0 };
        final static long SIZE = 8;

        /** We use a real pointer here because invalidation means calling the
         *  proxy's free() method.
         */
        //TODO Evaluate the need to cache the proxy of the object to invalidate
        private OffHeapObject oho;

        /* TODO Delete. Unneeded
        InvalidateEntry(long block) {
            super();
            setLongField( offsets[0], block );
        }
        */
        InvalidateEntry(OffHeapObject oho) {
            super();
            setHandleField( offsets[0], oho );
            this.oho = oho;
        }
        public InvalidateEntry(MemoryBlockHandle block) { super( null, block.getOffset() ); }
        public InvalidateEntry(Void v, long offset) { super( null, offset ); }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }
        public void descend() {
            //No-op;
        }
        private OffHeapObject getOho() {
            if( this.oho == null ) {
                this.oho = getHandleField( offsets[0] );
            }
            return this.oho;
        }

        /* TODO Delete. Unneeded
        public final MemoryBlockHandle getBlock() {
            return OffHeap.getAllocator()
                          .blockFromOffset( getLongField( offsets[0] ));
        }
        public void apply() { getBlock().init(); }
        */
        public void apply() { getOho().invalidate(); }
    }

    //Constructor
    public OffHeapRedoLog() {
        this( DEFAULT_SIZE );
    }
    public OffHeapRedoLog(int initialSize) {
        table = new OffHeapArray<>( initialSize );
        touched = new ArrayList<>();
        //OffHeap.Instances.put(table.getOffset(), this);
        OffHeap.getAllocator()
               .blockFromOffset( table.getOffset() ).setKlass( CLASS_ID );
    }
    //Reconstructor
    public OffHeapRedoLog(long offset) {
        table = (OffHeapArray<Entry>)OffHeapArray.rec( offset );
        touched = new ArrayList<>();
        //OffHeap.instances.put(table.getOffset(), this);
        //Prevent redo-ing when recovering from in-complete state
        MemoryBlockHandle headBlock = OffHeap.getAllocator().blockFromOffset( this.getOffset() );
        if( ! headBlock.isValid() ) {
            this.clear();
        }
    }
    public OffHeapRedoLog(MemoryBlockHandle block) {
        this( block.getOffset() );
    }
    public OffHeapRedoLog(Void v, long offset) {
        this( offset );
    }

    public void logCopy(long orig, long copy) {
        table.add( new CopyEntry( orig, copy ) );
    }
    public void logValidate(OffHeapObject oho) {
        table.add( new ValidateEntry( oho.getOffset() ) );
    }
    public void logInvalidate(OffHeapObject oho) {
        table.add( new InvalidateEntry( oho ) );
    }
    public void touch(OffHeapObject oho) {
        touched.add( oho );
    }

    /**
      * Invoked either after a crash, or at the end of an FA block.
      *   PRE _ entries written to log (validated)
      *     1 _ fence and validate log
      *     2 _ apply entries
      *     3 _ fence, invalidate log
      *     4 _ reset faBase indirection in affected proxies
      *  POST _ nothing, entries are cleared at the start of next FA block
      *
      */
    public void redo() {
        MemoryBlockHandle headBlock = OffHeap.getAllocator().blockFromOffset( this.getOffset() );
        this.fence();
        //this.validate(); but « raw » to avoid FA instrumentation
        headBlock.commit();
        applyEntries();
        this.fence();
        //this.invalidate(); but « raw » to avoid FA instrumentation
        headBlock.free();
        //reset FA state in affected proxies
        touched.forEach( OffHeapObject::resetFa );
        touched.clear();
    }

    /**
      * Invoked at the start of an FA block.
      */
    public void init() {
        this.clear();
    }

    private void applyEntries() {
        table.forEach( Entry::apply );
    }

    private void clear() {
        table.forEach( e -> e.destroy() );
        table.clear();
    }

    public long getOffset() { return table.getOffset(); }
    public void attach(long offset) { table.attach( offset ); }
    public void detach() { table.detach(); }
    public long classId() { return CLASS_ID; }
    public long length() { return table.length(); }
    public long addressFromFieldOffsetRO(long fieldOffset) {
        return table.addressFromFieldOffsetRO( fieldOffset );
    }
    public long addressFromFieldOffsetRW(long fieldOffset) {
        return table.addressFromFieldOffsetRW( fieldOffset );
    }
    public void validate() { table.validate(); }
    public void invalidate() { table.invalidate(); }
    public void resetFa() { table.resetFa(); }
    public void destroy() { table.destroy(); }
    public void flush() { table.flush(); }
    public boolean mark() { return table.mark(); }
    public void descend() { table.descend(); }

}
