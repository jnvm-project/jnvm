package eu.telecomsudparis.jnvm.offheap;


public class MemoryBlockHandle {

    //Transient
    private transient long offset = -1;

    public MemoryBlockHandle(long offset) {
        this.offset = offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getOffset() {
        return this.offset;
    }

    //Persistent layout
    private static final long HEADER = 0;
    private static final long BASE = HEADER + Header.SIZE;

    private class Header {
        public static final long SIZE = 8;

        //Flag masks
        public static final long VALIDITY   = 0x0000_0000_0000_0001L; // 0
        public static final long FORWARD    = 0x0000_0000_0000_0002L; // 1
        public static final long MULTIBLOCK = 0x0000_0000_0000_0004L; // 2

        //Value masks
        public static final long KLASS      = 0x0000_0000_0000_0F00L; // 8  - 11
        public static final long BSIZE      = 0x0000_0000_0000_F000L; // 12 - 15
        public static final long NEXT       = 0xFFFF_FFFF_FFFF_0000L; // 16 - 63

        //Value offsets
        public static final long oKLASS     = 8L;
        public static final long oBSIZE     = 12L;
        public static final long oNEXT      = 16L;
    }

    //Persistent fields
    public void setHeader(long header) {
        unsafe.putLong( offset + HEADER, header );
    }

    public long getHeader() {
        return unsafe.getLong( offset + HEADER );
    }

    public long base() {
        return this.offset + BASE;
    }

    //Header fields helpers
    private static boolean getFlag(long word, long flagMask) {
        return (word & flagMask) == flagMask;
    }

    private static long setFlag(long word, long flagMask) {
        return word | flagMask;
    }

    private static long clearFlag(long word, long flagMask) {
        return word & (~flagMask);
    }

    private static long setValue(long word, long value, long valueOffset, long valueMask) {
        return ( word & (~valueMask) )
               | (( value << valueOffset ) & valueMask );
    }

    private static long getValue(long word, long valueOffset, long valueMask) {
        return ( word & valueMask ) >> valueOffset;
    }

    private void replaceHeaderValue(long value, long valueOffset, long valueMask) {
        long h = getHeader();
        h = setValue(h, value, valueOffset, valueMask);
        setHeader( h );
    }

    private void replaceHeaderFlag(boolean value, long flagMask) {
        long h = getHeader();
        h = (value) ? setFlag(h, flagMask) : clearFlag(h, flagMask);
        setHeader( h );
    }

    //Header fields
    public boolean isValid() {
        return getFlag( getHeader(), Header.VALIDITY );
    }

    public boolean hasNext() {
        return getFlag( getHeader(), Header.FORWARD );
    }

    public boolean isMultiBlock() {
        return getFlag( getHeader(), Header.MULTIBLOCK );
    }

    public long getKlass() {
        return getValue( getHeader(), Header.oKLASS, Header.KLASS );
    }

    public long getBlockLength() {
        return getValue( getHeader(), Header.oBSIZE, Header.BSIZE );
    }

    public long getNext() {
        return getValue( getHeader(), Header.oNEXT, Header.NEXT );
    }

    public void setValid(boolean value) {
        replaceHeaderFlag( value, Header.VALIDITY );
    }

    public void setHasNext(boolean value) {
        replaceHeaderFlag( value, Header.FORWARD );
    }

    public void setMultiBlock(boolean value) {
        replaceHeaderFlag( value, Header.MULTIBLOCK );
    }

    public void setKlass(long klass) {
        replaceHeaderValue( klass, Header.oKLASS, Header.KLASS );
    }

    public void setBlockLength(long blockLength) {
        replaceHeaderValue( blockLength, Header.oBSIZE, Header.BSIZE );
    }

    public void setNext(long next) {
        replaceHeaderValue( next, Header.oNEXT, Header.NEXT );
    }

    //Static methods
    public static long size() {
        return SIZE;
    }

    public static void copy(long dest, long src) {
        unsafe.copyMemory( src, dest, SIZE );
    }

    //Instance methods
    public MemoryBlockHandle next() {
        return ( ! hasNext() ) ? null :
            OffHeap.getAllocator().blockFromOffset( getNext() );
    }

    public void commit() {
        setHeader( setFlag( getHeader(), Header.VALIDITY ) );
    }

    public void free() {
        setHeader( clearFlag( getHeader(), Header.VALIDITY ) );
    }

    public void init() {
        //unsafe.setMemory( offset, size(), (byte) 0 );
        setHeader( 0L );
    }

    public void flush() {
        //TODO only flush dirty cache lines
        unsafe.writebackMemory( offset, size() );
        unsafe.psync();
    }

    //Debug methods
    public String toString() {
        return String.format("Block at [%d] with header [%b,%b,%b,%s,%d,%d]",
            offset - OffHeap.baseAddr(),
            isValid(),
            hasNext(),
            isMultiBlock(),
            getKlass(),
            getBlockLength(),
            getNext());
    }

    //Unsafe mechanics
    private static final long SIZE;

    private static final sun.misc.Unsafe unsafe;
    static {
        unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();
        SIZE = 256;
    }

}
