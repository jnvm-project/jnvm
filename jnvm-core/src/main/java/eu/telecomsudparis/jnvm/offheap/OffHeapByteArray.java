package eu.telecomsudparis.jnvm.offheap;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class OffHeapByteArray
        extends OffHeapBigObjectHandle implements Iterable<Byte> {

    private static final long CLASS_ID = OffHeap.Klass.register( OffHeapByteArray.class );

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

    private int hash;

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
    public OffHeapByteArray(MemoryBlockHandle block) {
        OffHeapBigObjectHandle.rec( this, block.getOffset() );
    }
    public OffHeapByteArray(Void v, long offset) {
        OffHeapBigObjectHandle.rec( this, offset );
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

    @Override
    public int hashCode() {
        int h = hash;
        long length = length();
        if( h == 0 && length > 0 ) {
            for(int i=0; i < length; i++) {
               h = 31 * h + get( i );
            }
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object anObject) {
        System.out.println("toto");
        if( this == anObject ) {
            return true;
        }
        if( anObject instanceof OffHeapByteArray ) {
            OffHeapByteArray anOHByteArray = (OffHeapByteArray)anObject;
            long n = length();
            if( n == anOHByteArray.length() ) {
                for(long i=0; i<n; i++) {
                    if( this.get(i) != anOHByteArray.get(i) )
                        return false;
                }
                return true;
            }
        } else if( anObject instanceof byte[] ) {
            byte[] anByteArray = (byte[])anObject;
            long n = length();
            if( n == anByteArray.length ) {
                for(int i=0; i<n; i++) {
                    if( this.get(i) != anByteArray[i] )
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    public void descend() {
        //No-op;
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
