package eu.telecomsudparis.jnvm.offheap;

import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import java.lang.UnsupportedOperationException;
import java.util.NoSuchElementException;


public class OffHeapArray<E extends OffHeapObject>
        extends OffHeapBigObjectHandle implements Iterable<E> {

    private static final long CLASS_ID = OffHeap.Klass.register( OffHeapArray.class );

    /* PMEM Layout :
     *  | Index | Offset | Bytes | Name     |
     *  |-------+--------+-------+----------|
     *  | 0     | 0      | 8     | capacity |
     *  | 1     | 8      | 8     | length   |
     *  end: 16 bytes
     */
    private final static long[] offsets = { 0L, 8L };
    private static final long SIZE = 16;

    private static final long indexScale = Long.BYTES;
    private static final long baseOffset = SIZE;

    private long capacity() { return getLongField( offsets[0] ); }
    private void setCapacity(long capacity) { setLongField( offsets[0], capacity ); }
    public long length() { return getLongField( offsets[1] ); }
    private void setLength(long length) { setLongField( offsets[1], length); }
    private long incLength(long delta) { return getAndAddLongField( offsets[1], delta); }

    private void reset() { setLength( 0L ); }
    private static final long elemOffset(long index) { return baseOffset + index * indexScale; }
    private E getElem(long index) { return (E) getHandleField( elemOffset( index ) ); }
    private void setElem(long index, E e) { setHandleField( elemOffset( index ), e ); }
    private void unsetElem(long index) { setLongField( elemOffset( index ), -1); }

    //Constructor
    public OffHeapArray(long capacity) {
        super( computeSize( capacity ) );
        reset();
        setCapacity( capacity );
    }

    //Reconstructor
    private OffHeapArray() {}
    public static OffHeapArray rec(long offset) {
        return OffHeapBigObjectHandle.rec( new OffHeapArray<>(), offset );
    }
    public OffHeapArray(MemoryBlockHandle block) {
        OffHeapBigObjectHandle.rec( this, block.getOffset() );
    }

    //Instance methods
    protected static long computeSize(long length) {
        return SIZE + length * indexScale;
    }

    public long size() {
        return computeSize( length() );
    }

    public long indexScale() { return indexScale; }

    public long baseOffset() { return baseOffset; }

    public long classId() { return CLASS_ID; }

    public boolean contains(Object value) {
        return false;
        /*
        this.anyMatch( v -> v.equals( value ) );
        */
    }

    public E get(long index) {
        return getElem( index );
    }

    public long add(E e) {
        long index = incLength( 1 );
        setElem( index, e );
        return index;
    }

    public E remove(long index) {
        E oldValue = getElem( index );
        unsetElem( index );
        //TODO reshape array
        return oldValue;
    }

    public void clear() {
        reset();
    }

    public OffHeapArray clone() {
        return this;
    }

    public void cloneInto(OffHeapArray<E> dest) {
        for(long i=0; i<length(); i++) {
            dest.setElem( i, this.getElem( i ) );
        }
    }

    //Iterable methods
    public Iterator<E> iterator() {
        return new Iterator() {
            long cursor = 0;
            long end = length();
            public boolean hasNext() { return cursor < end; }
            public E next() {
                if( hasNext() ) {
/*
                    E e = getElem( cursor );
                    cursor += 1;
                    return e;
*/
                    return getElem( cursor++ );
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    public void descend() {
        for( E entry : this ) {
            entry.mark();
            entry.descend();
        }
    }

    /* Set methods */
    public Set<E> asSet() {
        return new Set<E>() {
            @Override
            public boolean add(E e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean contains(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int size() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<E> iterator() {
                return OffHeapArray.this.iterator();
            }

            @Override
            public Object[] toArray() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                throw new UnsupportedOperationException();
            }
        };
    }

}
