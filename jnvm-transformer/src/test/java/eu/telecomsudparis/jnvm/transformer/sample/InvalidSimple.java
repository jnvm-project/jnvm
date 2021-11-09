package eu.telecomsudparis.jnvm.transformer.sample;

import java.lang.String;

import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;

@Persistent(fa="non-private")
public class InvalidSimple {

    private String msg;
    private int x;
    private transient int y;

    public InvalidSimple(int x) {
        this.x = x;
        this.msg = new String("Hello_JNVM");
    }

    public void inc() { x++; }

}
