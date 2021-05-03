package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public class OffHeapString implements OffHeapObject, Comparable<OffHeapString> {

    private static final long CLASS_ID = OffHeap.Klass.register( OffHeapString.class );

    protected final OffHeapCharArray value;
    protected transient int hash;

    //Constructors
    protected OffHeapString(char[] original) {
        this.value = new OffHeapCharArray( original );
        //OffHeap.instances.put(value.getOffset(), this);
        OffHeap.getAllocator().blockFromOffset( value.getOffset() ).setKlass( CLASS_ID );
    }

    //Convertor
    public OffHeapString(String original) {
        this( original.toCharArray() );
        this.hash = original.hashCode();
    }

    public String toString() {
        return new String( value.toArray() );
    }

    //Reconstructor
    public OffHeapString(long offset) {
        this.value = OffHeapCharArray.rec( offset );
        //OffHeap.instances.put(value.getOffset(), this);
    }
    public OffHeapString(MemoryBlockHandle block) {
        this( block.getOffset() );
    }

    @Override
    public int hashCode() {
        int h = hash;
        if( h == 0 && value.length() > 0 ) {
            for(int i=0; i < value.length(); i++) {
               h = 31 * h + value.get( i );
            }
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object anObject) {
        if( this == anObject ) {
            return true;
        }
        if( anObject instanceof OffHeapString ) {
            OffHeapString anOHString = (OffHeapString)anObject;
            long n = value.length();
            if( n == anOHString.value.length() ) {
                for(int i=0; i<n; i++) {
                    if( this.value.get(i) != anOHString.value.get(i) )
                        return false;
                }
                return true;
            }
        } else if( anObject instanceof String ) {
            String anString = (String)anObject;
            long n = value.length();
            if( n == anString.length() ) {
                for(int i=0; i<n; i++) {
                    if( this.value.get(i) != anString.charAt(i) )
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    public char charAt(long index) {
        return value.get( index );
    }

    public int compareTo(OffHeapString anotherString) {
        long len1 = this.value.length();
        long len2 = anotherString.value.length();
        long lim = Math.min( len1, len2 );

        long k = 0;
        while( k < lim ) {
            char c1 = this.value.get(k);
            char c2 = anotherString.value.get(k);
            if( c1 != c2 ) {
                return c1 - c2;
            }
            k++;
        }
        return (int)( len1 - len2 );
    }

    public int compareTo(String anotherString) {
        int len1 = (int) this.value.length();
        int len2 = anotherString.length();
        int lim = Math.min( len1, len2 );

        int k = 0;
        while( k < lim ) {
            char c1 = this.value.get(k);
            char c2 = anotherString.charAt(k);
            if( c1 != c2 ) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    public long getOffset() { return value.getOffset(); }
    public void attach(long offset) { value.attach( offset ); }
    public void detach() { value.detach(); }
    public long size() { return value.size(); }
    public long classId() { return CLASS_ID; }
    public long length() { return value.length(); }
    public long addressFromFieldOffsetRO(long fieldOffset) {
        return value.addressFromFieldOffsetRO( fieldOffset );
    }
    public long addressFromFieldOffsetRW(long fieldOffset) {
        return value.addressFromFieldOffsetRW( fieldOffset );
    }
    public void validate() { value.validate(); }
    public void invalidate() { value.invalidate(); }
    public void destroy() { value.destroy(); }

}
