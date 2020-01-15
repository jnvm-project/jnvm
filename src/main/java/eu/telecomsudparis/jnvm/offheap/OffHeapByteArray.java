package eu.telecomsudparis.jnvm.offheap;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class OffHeapByteArray
        extends OffHeapBigObjectHandle implements Iterable<Byte> {

    /* OffHeap Layout :
     *  | Index | Offset | Bytes | Name   |
     *  |-------+--------+-------+--------|
     *  | 0     | 0      | 8     | length |
     *  end: 8 bytes
     */
    private final static long[] offsets = { 0L };
    private static final long SIZE = 8;

    private static final long indexScale = Byte.BYTES;
    private static final long baseOffset = SIZE;

    public long length() { return getLongField( offsets[0] ); }
    private void setLength(long length) { setLongField( offsets[0], length); }

    private void reset() { setLength( 0L ); }
    private static final long elemOffset(long index) { return baseOffset + index * indexScale; }
    private byte getElem(long index) { return getByteField( elemOffset( index ) ); }
    private void setElem(long index, byte e) { setByteField( elemOffset( index ), e ); }
    private void unsetElem(long index) { setByteField( elemOffset( index ), (byte) 0 ); }

    //Constructor
    public OffHeapByteArray(long length) {
        super( computeSize( length ) );
        setLength( length );
    }

    //Convertor
    public OffHeapByteArray(byte[] value) {
        this( value.length );
        for( int i=0; i < length(); i++ ) {
            setElem( i, value[i] );
        }
    }

    public byte[] toArray() {
        byte[] value = new byte[ (int) length() ];
        for( int i=0; i < length(); i++ ) {
            value[i] = getElem( i );
        }
        return value;
    }

    //Reconstructor
    private OffHeapByteArray() {}
    public static OffHeapByteArray rec(long offset) {
        return OffHeapBigObjectHandle.rec( new OffHeapByteArray(), offset );
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

    public byte get(long index) {
        return getElem( index );
    }

    public long add(byte e) {
        long index = length();
        setElem( index, e );
        setLength( index + 1 );
        return index;
    }

    public void set(long index, byte e) {
        setElem( index, e );
    }

    public void addAll(byte[] value) {
        for( int i=0; i < length(); i++ ) {
            setElem( i, value[i] );
        }
    }

    public byte remove(long index) {
        byte oldValue = getElem( index );
        unsetElem( index );
        //TODO reshape array
        return oldValue;
    }

    public void clear() {
        reset();
    }

    public OffHeapByteArray clone() {
        return this;
    }

    //Iterable methods
    public Iterator<Byte> iterator() {
        return new Iterator() {
            long cursor = 0;
            long end = length();
            public boolean hasNext() { return cursor < end; }
            public Byte next() {
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
