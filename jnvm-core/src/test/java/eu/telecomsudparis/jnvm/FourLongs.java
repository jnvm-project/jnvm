package eu.telecomsudparis.jnvm.jpa;

import java.io.Serializable;


public interface FourLongs extends Serializable {

    public long getL1();
    public long getL2();
    public long getL3();
    public long getL4();

    public void setL1(Long l);
    public void setL2(Long l);
    public void setL3(Long l);
    public void setL4(Long l);

}
