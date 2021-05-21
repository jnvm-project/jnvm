package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.MemoryBlockHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapArray;
import eu.telecomsudparis.jnvm.offheap.OffHeap;


public class OffHeapRedoLog implements OffHeapObject {
    private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.class );
    private static final int DEFAULT_SIZE = 16;

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
            setLongField( offsets[0], orig );
            setLongField( offsets[1], copy );
        }
        //Reconstructor
        public CopyEntry(MemoryBlockHandle block) { super( block.getOffset() ); }

        @Override
        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }

        public final long getOld() { return getLongField( offsets[0] ); }
        public final long getNew() { return getLongField( offsets[1] ); }
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
            setLongField( offsets[0], block );
        }
        public ValidateEntry(MemoryBlockHandle block) { super( block.getOffset() ); }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }

        public final MemoryBlockHandle getBlock() {
            return OffHeap.getAllocator()
                          .blockFromOffset( getLongField( offsets[0] ));
        }
        public void apply() { getBlock().commit(); getBlock().setRecordable( true ); }
//        public void apply() { getHandleField( offsets[0] ).validate(); }
    }

    public static class InvalidateEntry extends OffHeapObjectHandle implements Entry {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.InvalidateEntry.class );
        final static long[] offsets = { 0 };
        final static long SIZE = 8;

        private long block;

        InvalidateEntry(OffHeapObject oho) {
            super();
            setHandleField( offsets[0], oho );
        }
        public InvalidateEntry(MemoryBlockHandle block) { super( block.getOffset() ); }

        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }

/*
        public final MemoryBlockHandle getBlock() {
            return OffHeap.getAllocator()
                          .blockFromOffset( getLongField( offsets[0] ));
        }
        public void apply() { getBlock().init(); }
*/
        public void apply() { getHandleField( offsets[0] ).invalidate(); }
    }

    //Constructor
    public OffHeapRedoLog() {
        this( DEFAULT_SIZE );
    }
    public OffHeapRedoLog(int initialSize) {
        table = new OffHeapArray<>( initialSize );
        //OffHeap.Instances.put(table.getOffset(), this);
        OffHeap.getAllocator()
               .blockFromOffset( table.getOffset() ).setKlass( CLASS_ID );
    }
    //Reconstructor
    public OffHeapRedoLog(long offset) {
        table = (OffHeapArray<Entry>)OffHeapArray.rec( offset );
        //OffHeap.instances.put(table.getOffset(), this);
    }
    public OffHeapRedoLog(MemoryBlockHandle block) {
        this( block.getOffset() );
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

    public void redo() {
/*
        for( long i=0; i<table.length(); i++ ) {
            Entry e = table.get(i);
            e.apply();
        }
*/
        table.forEach( Entry::apply );
    }

    public void clear() {
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
    public void destroy() { table.destroy(); }
    public void flush() { table.flush(); }

}
