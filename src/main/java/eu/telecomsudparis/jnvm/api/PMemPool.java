package eu.telecomsudparis.jnvm.api;

import eu.telecomsudparis.jnvm.api.PMem;

public class PMemPool {

    public static final long BLOCK_SIZE = 256;

    //PMem layout
    private static long size_offset = 0;
    private static long position_offset = 8;
    private static long base_offset = 16;

    private transient long size_addr;
    private transient long position_addr;
    private transient long base_addr;

    //Transient attributes
    private transient String filename;
    private transient long limit;

    private transient long address = -1;
    private transient boolean loaded = false;

    public PMemPool(long limit, String filename) {
        this.limit = limit;
        this.filename = filename;
    }

    public void open() {
        assertNotLoaded();

        address = PMem.openPmemRoot( filename, limit );
        load();
    }

    public void close() {
        assertLoaded();

        PMem.freePmemRoot( address, limit );
        unload();
    }

    private void load() {
        size_addr = address + size_offset;
        position_addr = address + position_offset;
        base_addr = address + base_offset;

        loaded = true;
    }

    private void unload() {
        address = -1;
        size_addr = -1;
        position_addr = -1;
        base_addr = -1;

        loaded = false;
    }

    public boolean isLoaded() {
        return loaded;
    }

    private void assertLoaded() {
        if( !isLoaded() )
            throw new IllegalStateException( "PMemPool closed" );
    }

    private void assertNotLoaded() {
        if( isLoaded() )
            throw new IllegalStateException( "PMemPool opened" );
    }

    //Transient Getters
    public String getFilename() {
        return filename;
    }

    public long getLimit() {
        return limit;
    }

    public long getAddress() {
        return address;
    }

    //Persistent Getters
    public long getSize() {
        assertLoaded();

        //TODO Safe Persistent Get
        return unsafe.getLong( size_addr );
    }

    public long getPosition() {
        assertLoaded();

        //TODO Safe Persistent Get
        return unsafe.getLong( position_addr );
    }

    //Persistent Setters
    public void setSize(long size) {
        assertLoaded();

        //TODO Safe Persistent Set
        unsafe.putLong( size_addr, size );
    }

    public void setPosition(Long position) {
        assertLoaded();

        //TODO Safe Persistent Set
        unsafe.putLong( position_addr, position );
    }

    //Data manipulation methods
    private long block_offset(int index) {
        return index * BLOCK_SIZE;
    }

    private void putRaw(byte[] src, long offset, long bytes) {
        unsafe.copyMemory( src, arrayBaseOffset,
                           null, base_addr + offset, bytes );

        //TODO Atomic increment
        long old_size = getSize();
        setSize( old_size + 1 );
    }

    private void putRaw(byte[] src, long offset) {
        putRaw( src, offset, src.length * arrayIndexScale );
    }

    public void put(byte[] src, int index) {
        assertLoaded();

        putRaw( src, block_offset( index ) );
    }

    public void put(byte[] src) {
        assertLoaded();

        putRaw( src, getPosition() );

        //TODO Atomic increment
        long old_position = getPosition();
        setPosition( old_position + BLOCK_SIZE );
    }

    private void getRaw(byte[] dst, long offset, long bytes) {
        unsafe.copyMemory( null, base_addr + offset,
                           dst, arrayBaseOffset,
                           bytes );
    }

    private void getRaw(byte[] dst, long offset) {
        getRaw( dst, offset, dst.length * arrayIndexScale );
    }

    public void get(byte[] dst, int index) {
        assertLoaded();

        getRaw( dst, block_offset( index ) );
    }

    public void get(byte[] dst) {
        assertLoaded();

        getRaw( dst, getPosition() );

        //TODO Atomic increment
        long old_position = getPosition();
        setPosition( old_position + BLOCK_SIZE );
    }

    private void removeRaw(long offset) {
        unsafe.setMemory( base_addr + offset, BLOCK_SIZE, (byte) 0 );

        //TODO Atomic decrement
        long old_size = getSize();
        setSize( old_size - 1 );
    }

    public void remove(int index) {
        assertLoaded();

        removeRaw( block_offset( index ) );
    }

    public void rewind() {
        assertLoaded();

        setPosition( 0L );
    }

    public void clear(boolean erase) {
        assertLoaded();

        if( erase ) {
            unsafe.setMemory( address, limit, (byte) 0 );
        } else {
            setSize( 0L );
            setPosition( 0L );
        }
    }

    public void clear() {
        assertLoaded();

        clear( false );
    }

    // Unsafe Mechanics
    private static final sun.misc.Unsafe unsafe;
    private static final long arrayBaseOffset;
    private static final long arrayIndexScale;
    static {
        try {
            unsafe = net.bramp.unsafe.UnsafeHelper.getUnsafe();
            Class<?> k = byte[].class;
            arrayBaseOffset = unsafe.arrayBaseOffset( k );
            arrayIndexScale = unsafe.arrayIndexScale( k );
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
