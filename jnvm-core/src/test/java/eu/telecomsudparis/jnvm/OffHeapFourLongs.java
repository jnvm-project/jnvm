package eu.telecomsudparis.jnvm.jpa;

import java.util.Arrays;

import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;

public class OffHeapFourLongs extends OffHeapObjectHandle implements FourLongs {

    //OffHeapLayout
    //TODO Perhaps statically generate static fields (with appropriate accessors)
    private static final long[] offsets = { 0, 8, 16, 24 };
    private static final long SIZE = 32;

    //Constructor
    public OffHeapFourLongs(long l1, long l2, long l3, long l4) {
        super();
        setL1( l1 );
        setL2( l2 );
        setL3( l3 );
        setL4( l4 );
    }

    //Reconstructor
    public OffHeapFourLongs(long offset) {
        super( null, offset );
    }

    public long size() { return SIZE; }

    @Override
    public long getL1() { return getLongField( offsets[0] ); }
    @Override
    public long getL2() { return getLongField( offsets[1] ); }
    @Override
    public long getL3() { return getLongField( offsets[2] ); }
    @Override
    public long getL4() { return getLongField( offsets[3] ); }

    @Override
    public void setL1(Long l) { setLongField( offsets[0], l ); }
    @Override
    public void setL2(Long l) { setLongField( offsets[1], l ); }
    @Override
    public void setL3(Long l) { setLongField( offsets[2], l ); }
    @Override
    public void setL4(Long l) { setLongField( offsets[3], l ); }

    @Override
    public boolean equals(Object a) {
        if( this == a )
            return true;
        else if( a == null )
            return false;
/* Required for Interface-level equality
        else if( this.getClass() != a.getClass() )
            return false;
*/
        else if( !Arrays.stream( a.getClass().getInterfaces() )
                        .anyMatch( FourLongs.class::equals ) )
            return false;
        else {
            final FourLongs o = (FourLongs) a;
            return getL1() == o.getL1()
                && getL2() == o.getL2()
                && getL3() == o.getL3()
                && getL4() == o.getL4();
        }
    }

}
