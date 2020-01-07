package eu.telecomsudparis.jnvm.offheap;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class OffHeapArray<E extends OffHeapObjectHandle>
        extends OffHeapBigObjectHandle implements Iterable<E> {

    /* PMEM Layout :
     *  | Index | Offset | Bytes | Name   |
     *  |-------+--------+-------+--------|
     *  | 0     | 0      | 8     | length |
     *  end: 8 bytes
     */
    private final static long[] offsets = { 0L };
    private static final long SIZE = 8;

    private static final long indexScale = Long.BYTES;
    private static final long baseOffset = SIZE;

    public long length() { return getLongField( offsets[0] ); }
    private void setLength(long length) { setLongField( offsets[0], length); }

    private void reset() { setLength( 0L ); }
    private static final long elemOffset(long index) { return baseOffset + index * indexScale; }
    private E getElem(long index) { return (E) getHandleField( elemOffset( index ) ); }
    private void setElem(long index, E e) { setHandleField( elemOffset( index ), e ); }
    private void unsetElem(long index) { setLongField( elemOffset( index ), -1); }

    //Constructor
    public OffHeapArray(long capacity) {
        super( computeSize( capacity ) );
        reset();
    }

    //Reconstructor
    private OffHeapArray() {}
    public static OffHeapArray rec(long offset) {
        return OffHeapBigObjectHandle.rec( new OffHeapArray<>(), offset );
    }

    //Instance methods
    protected static long computeSize(long length) {
        return SIZE + length * indexScale;
    }

    public long size() {
        return computeSize( length() );
    }

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
        long index = length();
        setElem( index, e );
        setLength( index + 1 );
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

    //Iterable methods
    public Iterator<E> iterator() {
        return new Iterator() {
            long cursor = 0;
            long end = length();
            public boolean hasNext() { return cursor < end; }
            public E next() {
                if( hasNext() ) {
                    return getElem( cursor++ );
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    /* Set methods */
    /*
    @Override
    public boolean add(E e) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public Object[] toArray() {
        return null;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }
    */

}
