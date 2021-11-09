package eu.telecomsudparis.jnvm.transformer.sample;

import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;

@Persistent(fa="non-private")
public class Simple {

    private OffHeapString msg;
    private int x;
    private transient int y;

    public Simple(int x) {
        this.x = x;
        this.msg = new OffHeapString("Hello_JNVM");
    }

    public void inc() { x++; }

}
