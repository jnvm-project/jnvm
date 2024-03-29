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
    //In essence, mappings is only useful if offsets are someday made virtual.

    //Constructor
    //TODO Should be private.
    //Allocator can only be safely be used after complete recovery, thus
    //should only be instantiable from static recovery method.
    public MemoryAllocator(long offset, long limit) {
        this.totalMemory = limit - BASE;
        this.offset = offset;
        this.reclaimed = new ConcurrentLinkedDeque<>();
        //this.mappings = new ConcurrentHashMap<>();
    }

    //Reconstructor
    /**
      * Older recovery procedure, where heap could be recovered only from
      * validity bits.
      *
      * Linear heap scan only.
      * Every invalid block is salvaged.
      * Every valid block (master) is ressurected s.t. the object may fix
      *   its state.
      * Multiblocks (slave) are safely ignored (fixed by master already).
      *
      * This procedure assumes no memory leaks may happen during execution,
      * i.e. object pointers should always be updated atomically and
      * crash-consistently.
      * This was achieved with either failure-atomic blocks, or atomic helpers.
      * The atomic helper was using the « next » bits in a block's header to
      * store a pointer to the new version of the block, making it possible to
      * retrieve either the old version or the new one after a crash.
      *
      * Deprecated since we decided to allow relaxed pointer updates and that
      * requires fixing the potential leaks with a costlier recovery procedure.
      *
      */
    private static MemoryAllocator recover(long offset, long limit) {
        MemoryAllocator allocator = new MemoryAllocator(offset, limit);

        //Reconstruct « reclaimed » and « mappings » datastructs
        for(MemoryBlockHandle block : allocator) {
            if( block.isValid() && block.hasNext() && block.next().isValid() ) {
                //Invalidate when new version is ready but old was not dismissed
                block.free();
            }
            if( !block.isValid() ) {
                allocator.reclaimed.add( block );
            } else if( !block.isMultiBlock() ) {
                //Ressurect instance for fixup
                OffHeap.newInstance( block );
            } else {
                //allocator.mappings.put( block.getOffset(), block );
            }
        }

        return allocator;
    }
    /**
      * More recent recovery procedure, takes in the bitmap of blocks reachable
      * from the root objects to reconstruct the free list of the allocator.
      *
      * Object graph walk as heap scan.
      * Objects fixes themselves as walking through the graph ressurects them.
      * Every block not marked in the bitset is unreachable and garbage -
      *   from a previous run, or earlier object fixup.
      * Invalid blocks are directely salvaged. Valid ones are freed first.
      *
      * This procedure presents no challenge and allows for a more relaxed
      * programming experience, but currently, generating the BitSet is
      * sub-optimal. We could go plenty faster.
      *
      * TODO Write a graph-walk from the root objects, that works statically
      * (does not rely on proxies, walk through by directly reading the memory).
      * Either in one of those two fashions.
      *  1. Traverse blocks and multiblocks, reading class id in header to get
      *     which offset in the layout is a pointer and continue.
      *  2. Traverse blocks and multiblocks, ressurect objects 1 at a time for
      *     fixup and children addresses, discard proxy and continue.
      *
      * Recursively walking the graph using methods from the proxies is slower,
      * and may instantiate a large number of objects, when really, we don't
      * have to. Plus, it may interfer with the patterns from the application
      * for retaining/discarding object proxies.
      *
      */
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
        allocator.setTop( endIdx * MemoryBlockHandle.size() );
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
    //TODO Find a use for SIZE field, or drop it.

/*
    public long size() {
        return unsafe.getLong( offset + SIZE );
    }
*/

    public long top() {
        return unsafe.getLong( offset + TOP );
    }

    private void setTop(long newTop) {
        unsafe.putLong( offset + TOP, newTop );
    }

    //Instance Methods
    public MemoryBlockHandle allocateBlock() {
        MemoryBlockHandle block = reclaimed.poll();
        if( block == null ) {
            long blockOffset = unsafe.getAndAddLong( null, offset + TOP, MemoryBlockHandle.size() );
            //TOP is recomputed on bootstrap, no need to flush
            //unsafe.pwb( offset + TOP );
            block = new MemoryBlockHandle( offset + BASE + blockOffset );
        } else {
            //Always fully clear recycled blocks
            block.clear();
        }
        block.init();
        block.commit();

        //unsafe.getAndAddLong( null, offset + SIZE, block.size() );

        //mappings.put( block.getOffset(), block );

        return block;
    }

    public void freeBlock(MemoryBlockHandle block) {
        //block.free();
        block.init();
        //TODO Decide on clearing validity or whole block header
        //We don't have to flush, blocks are always cleared on allocation

        //unsafe.getAndAddLong( null, offset + SIZE, -block.size() );

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

/*
    public long availableMemory() {
        return totalMemory - size();
    }
*/

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
