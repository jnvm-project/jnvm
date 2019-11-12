package eu.telecomsudparis.jnvm.offheap;

import java.util.LinkedList;
import java.util.HashMap;


public class MemoryAllocator {

    //Transient
    private transient long offset = -1;

    private transient long totalMemory;
    private transient LinkedList<MemoryBlockHandle> reclaimed;
    private transient HashMap<Long, MemoryBlockHandle> mappings;
    //TODO Can get rid of mappings, as long as MemoryBlocks are interchangeable

    //Constructor
    private MemoryAllocator(long offset, long limit) {
        this.totalMemory = limit - BASE;
        this.offset = offset;
        this.reclaimed = new LinkedList<>();
        this.mappings = new HashMap<>();
    }

    //Reconstructor
    public static MemoryAllocator recover(long offset, long limit) {
        MemoryAllocator allocator = new MemoryAllocator(offset, limit);

        //Reconstruct « reclaimed » and « mappings » datastructs
        for(long i=0; i < allocator.top(); i += MemoryBlockHandle.size() ) {
            MemoryBlockHandle block = new MemoryBlockHandle( offset + BASE + i );
            if( block.isValid() && block.hasNext() && block.next().isValid() ) {
                //Invalidate when new version is ready but old was not dismissed
                block.free();
            }
            if( !block.isValid() ) {
                allocator.reclaimed.add( block );
            } else {
                allocator.mappings.put( block.getOffset(), block );
            }
        }

        return allocator;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getOffset() {
        return this.offset;
    }

    //Persistent Layout
    private static final long SIZE = 0;
    private static final long TOP = SIZE + Long.BYTES;
    private static final long BASE = TOP + Long.BYTES;

    public long size() {
        return unsafe.getLong( offset + SIZE );
    }

    public long top() {
        return unsafe.getLong( offset + TOP );
    }

    //Instance Methods
    public MemoryBlockHandle allocateBlock() {
        MemoryBlockHandle block = null;
        if( reclaimed.size() > 0 ) {
            block = reclaimed.remove();
        } else {
            long blockOffset = top();
            unsafe.putLong( offset + TOP, blockOffset + block.size() );
            block = new MemoryBlockHandle( offset + BASE + blockOffset );
        }
        block.init();

        unsafe.putLong( offset + SIZE, this.size() + block.size() );

        mappings.put( block.getOffset(), block );

        return block;
    }

    public void freeBlock(MemoryBlockHandle block) {
        block.free();

        unsafe.putLong( offset + SIZE, this.size() - block.size() );

        reclaimed.add( block );
    }

    public MemoryBlockHandle blockFromOffset(long offset) {
        return mappings.get( offset );
    }

    //MemoryAllocator methods
    public long allocateMemory(long size) {
        return allocateBlock().getOffset();
    }

    public void freeMemory(long offset, long size) {
        MemoryBlockHandle block = mappings.remove( offset );
        freeBlock( block );
    }

    public long totalMemory() {
        return totalMemory;
    }

    public long availableMemory() {
        return totalMemory - size();
    }

    public long usedMemory() {
        return top();
    }

    //Unsafe mechanics
    private static final sun.misc.Unsafe unsafe;
    static {
        unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();
    }

}
