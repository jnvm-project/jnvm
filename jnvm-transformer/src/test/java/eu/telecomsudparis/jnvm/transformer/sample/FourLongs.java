package eu.telecomsudparis.jnvm.transformer.sample;

import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;

@Persistent(fa="non-private")
public class FourLongs {

    private long l1;
    private long l2;
    private long l3;
    private long l4;

    public FourLongs(Long l1, Long l2, Long l3, Long l4) {
        this.l1 = l1;
        this.l2 = l2;
        this.l3 = l3;
        this.l4 = l4;
    }

    public long sum() { return l1 + l2 + l3 + l4; }

    public void add(long l1, long l2, long l3, long l4){
        this.l1+=l1;
        this.l2+=l2;
        this.l3+=l3;
        this.l4+=l4;
    }

    public void add(FourLongs fourLongs) {
        add(fourLongs.l1, fourLongs.l2, fourLongs.l3, fourLongs.l4);
    }

    public boolean equals(Object a) {
        if( this == a )
            return true;
        else if( a == null )
            return false;
        else {
            final FourLongs o = (FourLongs) a;
            return l1 == o.l1
                && l2 == o.l2
                && l3 == o.l3
                && l4 == o.l4;
        }
    }

}
