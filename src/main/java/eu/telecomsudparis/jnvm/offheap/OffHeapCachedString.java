package eu.telecomsudparis.jnvm.offheap;

import eu.telecomsudparis.jnvm.offheap.OffHeap;


public class OffHeapCachedString extends OffHeapString implements Comparable<OffHeapString> {

    private static final long CLASS_ID = OffHeap.Klass.register( OffHeapCachedString.class );

    private final transient char[] value;

    //Constructors
    protected OffHeapCachedString(char[] original) {
        super( original );
        this.value = original;
        //OffHeap.instances.put(value.getOffset(), this);
        OffHeap.getAllocator().blockFromOffset( super.value.getOffset() ).setKlass( CLASS_ID );
    }

    //Convertors
    public OffHeapCachedString(String original) {
        this( original.toCharArray() );
        super.hash = original.hashCode();
    }

    @Override
    public String toString() {
        return new String( value );
    }

    //Reconstructor
    public OffHeapCachedString(long offset) {
        super(offset);
        this.value = super.value.toArray();
    }
    public OffHeapCachedString(MemoryBlockHandle block) {
        this( block.getOffset() );
    }
    public OffHeapCachedString(Void v, long offset) {
        this( offset );
    }

    @Override
    public int hashCode() {
        int h = hash;
        int n = value.length;
        if( h == 0 && n > 0 ) {
            for(int i=0; i < n; i++) {
               h = 31 * h + value[ i ];
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
        if( anObject instanceof OffHeapCachedString ) {
            OffHeapCachedString anOHCString = (OffHeapCachedString)anObject;
            long n = value.length;
            if( n == anOHCString.value.length ) {
                for(int i=0; i<n; i++) {
                    if( this.value[i] != anOHCString.value[i] )
                        return false;
                }
                return true;
            }
        } else if( anObject instanceof OffHeapString ) {
            OffHeapString anOHString = (OffHeapString)anObject;
            long n = value.length;
            if( n == anOHString.value.length() ) {
                for(int i=0; i<n; i++) {
                    if( this.value[i] != anOHString.value.get(i) )
                        return false;
                }
                return true;
            }
        } else if( anObject instanceof String ) {
            String anString = (String)anObject;
            long n = value.length;
            if( n == anString.length() ) {
                for(int i=0; i<n; i++) {
                    if( this.value[i] != anString.charAt(i) )
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public char charAt(long index) {
        return value[ (int) index ];
    }

    @Override
    public int compareTo(OffHeapString anotherString) {
        int len1 = this.value.length;
        int len2 = (int) anotherString.length();
        int lim = Math.min( len1, len2 );

        int k = 0;
        while( k < lim ) {
            char c1 = this.value[ k ];
            char c2 = anotherString.charAt( k );
            if( c1 != c2 ) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    public int compareTo(String anotherString) {
        int len1 = (int) this.value.length;
        int len2 = anotherString.length();
        int lim = Math.min( len1, len2 );

        int k = 0;
        while( k < lim ) {
            char c1 = this.value[ k ];
            char c2 = anotherString.charAt( k );
            if( c1 != c2 ) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    @Override
    public long classId() { return CLASS_ID; }
    @Override
    public long length() { return value.length; }

}
