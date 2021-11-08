package eu.telecomsudparis.jnvm.jpa;

import java.util.Arrays;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class OnHeapFourLongs implements FourLongs {
    @Id
    private long l1;
    private long l2;
    private long l3;
    private long l4;

    public OnHeapFourLongs(Long l1, Long l2, Long l3, Long l4) {
        this.l1 = l1;
        this.l2 = l2;
        this.l3 = l3;
        this.l4 = l4;
    }

    @Override
    public long getL1() { return l1; }
    @Override
    public long getL2() { return l2; }
    @Override
    public long getL3() { return l3; }
    @Override
    public long getL4() { return l4; }

    @Override
    public void setL1(Long l) { l1 = l; }
    @Override
    public void setL2(Long l) { l2 = l; }
    @Override
    public void setL3(Long l) { l3 = l; }
    @Override
    public void setL4(Long l) { l4 = l; }

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
