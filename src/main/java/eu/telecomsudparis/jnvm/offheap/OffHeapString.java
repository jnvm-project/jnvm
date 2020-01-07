package eu.telecomsudparis.jnvm.offheap;


public class OffHeapString implements OffHeapObject {

    private final OffHeapCharArray value;
    private transient int hash;

    //Constructors
    public OffHeapString(OffHeapString original) {
        this.value = original.value;
        this.hash = original.hash;
    }

    //Convertor
    public OffHeapString(String original) {
        this.value = new OffHeapCharArray( original.toCharArray() );
        this.hash = original.hashCode();
    }

    public String toString() {
        return new String( value.toArray() );
    }

    //Reconstructor
    public OffHeapString(long offset) {
        this.value = OffHeapCharArray.rec( offset );
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
            char v2[] = anString.toCharArray();
            if( n == v2.length ) {
                for(int i=0; i<n; i++) {
                    if( this.value.get(i) != v2[i] )
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    public long getOffset() { return value.getOffset(); }
    public void attach(long offset) { value.attach( offset ); }
    public void detach() { value.detach(); }
    public long size() { return value.size(); }
    public long length() { return value.length(); }
    public long addressFromFieldOffset(long fieldOffset) {
        return value.addressFromFieldOffset( fieldOffset );
    }
    public void destroy() { value.destroy(); }

}