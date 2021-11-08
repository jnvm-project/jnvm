package eu.telecomsudparis.jnvm.offheap;

import java.util.concurrent.ConcurrentHashMap;

import eu.telecomsudparis.jnvm.PMem;


public class MemoryPool {

    private static ConcurrentHashMap<String, MemoryPool> pools =
        new ConcurrentHashMap<>();

    protected static MemoryPool open(String path, long limit) {
        MemoryPool pool = pools.get( path );
        if( pool == null ) {
            pool = new MemoryPool( path, limit );
            pools.put( path, pool );
        }
        if ( !pool.isOpen() ) {
            pool.open();
        }
        return pool;
    }

    protected static void close(String path) {
        MemoryPool pool = pools.get( path );
        if( pool != null && pool.isOpen() ) {
            pool.close();
        }
    }

    private transient String path;
    private transient long limit;
    private transient long address = -1;
    private transient boolean open = false;

    private MemoryPool(String path, long limit) {
        this.limit = limit;
        this.path = path;
    }

    private void assertOpen() {
        if( !isOpen() )
            throw new IllegalStateException( "MemoryPool is closed" );
    }

    private void assertNotOpen() {
        if( isOpen() )
            throw new IllegalStateException( "MemoryPool is open" );
    }

    protected boolean isOpen() {
        return open;
    }

    protected long address() {
        assertOpen();

        return this.address;
    }

    protected long limit() {
        return this.limit;
    }

    private void open() {
        assertNotOpen();

        address = PMem.openPmemRoot( path, limit );
        load();
    }

    protected void close() {
        assertOpen();

        PMem.freePmemRoot( address, limit );
        unload();
    }

    private void load() {
        open = true;
    }

    private void unload() {
        address = -1;
        open = false;
    }

}
