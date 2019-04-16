package eu.telecomsudparis.jnvm.api;

public class PMem {
    static { System.loadLibrary("jnvm-jni"); }

    public native long openPmemRoot(String path, long size);
    public native void freePmemRoot(long addr, long size);
    //TODO closePmemRoot from only path
}
