package eu.telecomsudparis.jnvm.api;

public class PMem {
    static { System.loadLibrary("jnvm-jni"); }

    public static native long openPmemRoot(String path, long size);
    public static native void freePmemRoot(long addr, long size);
    //TODO closePmemRoot from only path
}
