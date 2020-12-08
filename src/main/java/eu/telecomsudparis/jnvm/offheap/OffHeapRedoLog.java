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

    public static abstract class Entry extends OffHeapObjectHandle {
        final static long[] offsets = { 0 };
        final static long SIZE = 8;

        //Constructor
        Entry(long block) {
            super();
            setLongField( offsets[0], block );
        }
        //Reconstructor
        Entry(MemoryBlockHandle block) { super( block.getOffset() ); }

        public long size() { return SIZE; }

        public final MemoryBlockHandle getBlock() {
            return OffHeap.getAllocator()
                          .blockFromOffset( getLongField( offsets[0] ));
        }
        public abstract void apply();
    }

    public static class CopyEntry extends Entry {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.CopyEntry.class );
        final static long[] offsets = { 0, 8 };
        final static long SIZE = 16;

        //Constructor
        CopyEntry(long orig, long copy) {
            super( orig );
            setLongField( offsets[1], copy );
        }
        //Reconstructor
        CopyEntry(MemoryBlockHandle block) { super( block ); }

        @Override
        public long size() { return SIZE; }
        public long classId() { return CLASS_ID; }

        public final long getOld() { return getLongField( offsets[0] ); }
        public final long getNew() { return getLongField( offsets[1] ); }
        public void apply() { MemoryBlockHandle.copy(getOld(), getNew()); }
    }

    public static class ValidateEntry extends Entry {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.ValidateEntry.class );

        ValidateEntry(long block) { super( block ); }
        ValidateEntry(MemoryBlockHandle block) { super( block ); }

        public long classId() { return CLASS_ID; }

        public void apply() { getBlock().commit(); }
    }

    public static class InvalidateEntry extends Entry {
        private static final long CLASS_ID = OffHeap.Klass.register( OffHeapRedoLog.InvalidateEntry.class );

        private long block;

        InvalidateEntry(long block) { super( block ); }
        InvalidateEntry(MemoryBlockHandle block) { super( block ); }

        public long classId() { return CLASS_ID; }

        public void apply() { getBlock().init(); }
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
    public void logValidate(long block) {
        table.add( new ValidateEntry( block ) );
    }
    public void logInvalidate(long block) {
        table.add( new InvalidateEntry( block ) );
    }

    public void redo() {
        table.forEach( Entry::apply );
    }

    public void clear() {
        table.clear();
    }

    public long getOffset() { return table.getOffset(); }
    public void attach(long offset) { table.attach( offset ); }
    public void detach() { table.detach(); }
    public long classId() { return CLASS_ID; }
    public long length() { return table.length(); }
    public long addressFromFieldOffset(long fieldOffset) {
        return table.addressFromFieldOffset( fieldOffset );
    }
    public void destroy() { table.destroy(); }

}
