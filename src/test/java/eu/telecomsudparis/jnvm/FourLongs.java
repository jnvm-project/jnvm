package eu.telecomsudparis.jnvm.api.jpa;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class FourLongs implements Serializable {
    @Id
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

    public Long getL1() { return l1; }
    public Long getL2() { return l2; }
    public Long getL3() { return l3; }
    public Long getL4() { return l4; }

    public void setL1(Long l) { l1 = l; }
    public void setL2(Long l) { l2 = l; }
    public void setL3(Long l) { l3 = l; }
    public void setL4(Long l) { l4 = l; }

    public boolean equals(FourLongs o) {
        return l1 == o.l1 && l2 == o.l2 && l3 == o.l3 && l4 == o.l4;
    }

}
