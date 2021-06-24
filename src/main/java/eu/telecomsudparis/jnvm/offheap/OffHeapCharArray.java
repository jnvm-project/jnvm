package eu.telecomsudparis.jnvm.offheap;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class OffHeapCharArray
        extends OffHeapBigObjectHandle implements Iterable<Character> {

    private static final long CLASS_ID = OffHeap.Klass.register( OffHeapCharArray.class );

    /* OffHeap Layout :
     *  | Index | Offset | Bytes | Name   |
     *  |-------+--------+-------+--------|
     *  | 0     | 0      | 8     | length |
     *  end: 8 bytes
     */
    private final static long[] offsets = { 0L };
    private static final long SIZE = 8;

    private static final long indexScale = Character.BYTES;
    private static final long baseOffset = SIZE;
    private static final long nativeIndexScale =
        unsafe.arrayIndexScale(char[].class);
    private static final long nativeBaseOffset =
        unsafe.arrayBaseOffset(char[].class);
    private static final long ELEM_FIRST_BASE =
        ( OffHeapBigObjectHandle.BYTES_PER_BASE - baseOffset ) / indexScale;
    private static final long ELEM_PER_BASE =
        ( OffHeapBigObjectHandle.BYTES_PER_BASE ) / indexScale;

    public long length() { return getLongField( offsets[0] ); }
    private void setLength(long length) { setLongField( offsets[0], length); }

    private void reset() { setLength( 0L ); }
    private static final long elemOffset(long index) { return baseOffset + index * indexScale; }
    private char getElem(long index) { return getCharField( elemOffset( index ) ); }
    private void setElem(long index, char e) { setCharField( elemOffset( index ), e ); }
    private void unsetElem(long index) { setCharField( elemOffset( index ), '\u0000' ); }

    //Constructor
    public OffHeapCharArray(long length) {
        super( computeSize( length ) );
        setLength( length );
    }

    //Convertor
    // Naive
    public OffHeapCharArray(char[] value) {
        this( value.length );
        for( int i=0; i < value.length; i++ ) {
            setElem( i, value[i] );
        }
    }
    // Unsafe
    /*
    public OffHeapCharArray(char[] value) {
        this( value.length );
        long[] bases = this.getBases();
        long nOffset = nativeBaseOffset;

        unsafe.copyMemory( value, nOffset, null, bases[0] + 16, ELEM_FIRST_BASE * nativeIndexScale );
        nOffset += ELEM_FIRST_BASE * nativeIndexScale;
        int iter=1;
        for( long i=ELEM_FIRST_BASE; i < value.length; i+=ELEM_PER_BASE ) {
            unsafe.copyMemory( value, nOffset, null, bases[iter] + 8, ELEM_PER_BASE * nativeIndexScale );
            nOffset += ELEM_PER_BASE * nativeIndexScale;
            iter++;
        }
    }
    */

    public char[] toArray() {
        char[] value = new char[ (int) length() ];
        for( int i=0; i < length(); i++ ) {
            value[i] = getElem( i );
        }
        return value;
    }

    //Reconstructor
    private OffHeapCharArray() {}
    public static OffHeapCharArray rec(long offset) {
        return OffHeapBigObjectHandle.rec( new OffHeapCharArray(), offset );
    }
    public OffHeapCharArray(MemoryBlockHandle block) {
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

    public char get(long index) {
        return getElem( index );
    }

    public long add(char e) {
        long index = length();
        setElem( index, e );
        setLength( index + 1 );
        return index;
    }

    public char remove(long index) {
        char oldValue = getElem( index );
        unsetElem( index );
        //TODO reshape array
        return oldValue;
    }

    public void clear() {
        reset();
    }

    public OffHeapCharArray clone() {
        return this;
    }

    public OffHeapCharArray copy() {
        long len = this.length();
        OffHeapCharArray ret = new OffHeapCharArray( len );
        for( long i=0; i<len; i++) {
            ret.setElem( i, this.getElem(i) );
        }
        return ret;
    }

    //Iterable methods
    public Iterator<Character> iterator() {
        return new Iterator() {
            long cursor = 0;
            long end = length();
            public boolean hasNext() { return cursor < end; }
            public Character next() {
                if( hasNext() ) {
                    return getElem( cursor++ );
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
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
