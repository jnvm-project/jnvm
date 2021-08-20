package eu.telecomsudparis.jnvm.offheap;

import java.util.Map;
import java.util.Queue;
import java.util.BitSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class MemoryAllocator implements Iterable<MemoryBlockHandle> {

    //Transient
    private transient long offset = -1;

    private transient long totalMemory;
    private transient Queue<MemoryBlockHandle> reclaimed;
    //private transient Map<Long, MemoryBlockHandle> mappings;
    //TODO Can get rid of mappings, as long as MemoryBlocks are interchangeable

    //Constructor
    public MemoryAllocator(long offset, long limit) {
        this.totalMemory = limit - BASE;
        this.offset = offset;
        this.reclaimed = new ConcurrentLinkedDeque<>();
        //this.mappings = new ConcurrentHashMap<>();
    }

    //Reconstructor
    public static void recover(MemoryAllocator allocator) {
    //public static MemoryAllocator recover(long offset, long limit) {
    //    MemoryAllocator allocator = new MemoryAllocator(offset, limit);

        //Reconstruct « reclaimed » and « mappings » datastructs
        for(MemoryBlockHandle block : allocator) {
            if( block.isValid() && block.hasNext() && block.next().isValid() ) {
                //Invalidate when new version is ready but old was not dismissed
                block.free();
            }

            if( !block.isValid() ) {
                allocator.reclaimed.add( block );
            } else if( !block.isMultiBlock() ) {
                //OffHeap.newInstance( block );
            } else {
                //allocator.mappings.put( block.getOffset(), block );
            }
        }

        //return allocator;
    }
    public static void recover(MemoryAllocator allocator, BitSet marks) {
        //long endIdx = allocator.top() / MemoryBlockHandle.size();
        long endIdx = marks.length();
        long idx = marks.nextClearBit( 0 );
        while( idx < endIdx ) {
            long blockAddr = allocator.offset + BASE + idx * MemoryBlockHandle.size();
            MemoryBlockHandle block = new MemoryBlockHandle( blockAddr );
            //System.out.println(idx + " : " + OffHeap.Klass.klazz( block.getKlass() ) + " : " + block);
            if( block.isValid() ) {
                allocator.freeBlock( block );
            } else {
                allocator.reclaimed.add( block );
            }
            idx = marks.nextClearBit( (int)( idx + 1 ) );
        }
        /*
        System.out.println("reclaimed : " + allocator.reclaimed.size());
        System.out.println("top : " + allocator.top() );
        System.out.println("marks.length() : " + endIdx * MemoryBlockHandle.size() );
        */
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
        MemoryBlockHandle block = reclaimed.poll();
        if( block == null ) {
            long blockOffset = unsafe.getAndAddLong( null, offset + TOP, block.size() );
            block = new MemoryBlockHandle( offset + BASE + blockOffset );
        } else {
            block.clear();
        }
        block.init();
        block.commit();

        unsafe.getAndAddLong( null, offset + SIZE, block.size() );

        //mappings.put( block.getOffset(), block );

        return block;
    }

    public void freeBlock(MemoryBlockHandle block) {
        block.free();

        unsafe.getAndAddLong( null, offset + SIZE, -block.size() );

        reclaimed.add( block );
    }

    public MemoryBlockHandle blockFromOffset(long offset) {
        //return mappings.get( offset );
        return new MemoryBlockHandle( offset );
    }

    //MemoryAllocator methods
    public long[] allocateMemory(long size, long[] blockOffsets) {
        for(int i=0; i<blockOffsets.length; i++) {
            blockOffsets[ i ] = allocateBlock().getOffset();
        }
        return blockOffsets;
    }

    public void freeMemory(long[] blockBases) {
        for(int i=0; i<blockBases.length; i++) {
            long off = blockBases[ i ] - 8;
            MemoryBlockHandle block = new MemoryBlockHandle( off );
            freeBlock( block );
        }
    }

    public long[] allocateMemory(long size) {
        long nblocks = size / (MemoryBlockHandle.size() - 8) + 1;
        long[] blockOffsets = new long[ (int) nblocks ];
        return allocateMemory( size, blockOffsets );
    }

    public void freeMemory(long offset, long size) {
        long nblocks = size / (MemoryBlockHandle.size() - 8) + 1;
        long off = offset;
        do {
            //MemoryBlockHandle block = mappings.remove( off );
            MemoryBlockHandle block = new MemoryBlockHandle( off );
            off = OffHeap.baseAddr() + unsafe.getLong( block.base() );
            freeBlock( block );
            nblocks--;
        } while( nblocks > 0 );
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

    //Iterable methods
    public Iterator<MemoryBlockHandle> iterator() {
        return new Iterator() {
            long cursor = 0;
            long increment = MemoryBlockHandle.size();
            long end = MemoryAllocator.this.top();
            public boolean hasNext() { return cursor < end; }
            public MemoryBlockHandle next() {
                if( hasNext() ) {
                    MemoryBlockHandle block = new MemoryBlockHandle( offset + BASE + cursor );
                    cursor += increment;
                    return block;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    //Unsafe mechanics
    private static final sun.misc.Unsafe unsafe;
    static {
        unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();
    }

}
