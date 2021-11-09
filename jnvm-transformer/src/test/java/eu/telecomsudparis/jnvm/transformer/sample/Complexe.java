package eu.telecomsudparis.jnvm.transformer.sample;

import java.lang.String;

import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;

@Persistent
public class Complexe extends Simple {

    private byte by;
    private short s;
    private int i;
    private long l;
    private float f;
    private double d;
    private char c;
    private OffHeapString str;
    private boolean bo;

    private transient byte tby;
    private transient short ts;
    private transient int ti;
    private transient long tl;
    private transient float tf;
    private transient double td;
    private transient char tc;
    private transient String tstr;
    private transient boolean tbo;

    public Complexe(byte by, short s, int i, long l, float f, double d, char c, String str, boolean bo) {
        super(0);
        this.by = by;
        this.s = s;
        this.i = i;
        this.l = l;
        this.f = f;
        this.d = d;
        this.c = c;
        this.str = new OffHeapString(str);
        this.bo = bo;
    }

    private void cache() {
        this.tby = by;
        this.ts = s;
        this.ti = i;
        this.tl = l;
        this.tf = f;
        this.td = d;
        this.tc = c;
        this.tstr = str.toString();
        this.tbo = bo;
    }

    public void inc() {
        cache();
        this.by++;
        this.s++;
        this.i++;
        this.l++;
        this.f++;
        this.d++;
        this.c++;
        /*
        this.str++;
        this.bo++;
        */
    }

}
