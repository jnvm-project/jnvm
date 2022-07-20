package eu.telecomsudparis.jnvm;

import java.lang.UnsatisfiedLinkError;
import java.lang.ExceptionInInitializerError;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

public class PMem {
    static {
      try {
        System.loadLibrary("jnvm-jni");
      } catch(UnsatisfiedLinkError e) {
        try {
          InputStream is = PMem.class.getResourceAsStream("/libjnvm-jni.so");
          File file = File.createTempFile("jnvm-jni", ".so");
          OutputStream os = new FileOutputStream(file);
          byte[] buffer = new byte[4096];
          int length;
          while ((length = is.read(buffer)) != -1) {
            os.write(buffer, 0, length);
          }
          System.load(file.getAbsolutePath());
          os.close();
          file.deleteOnExit();
          is.close();
        } catch(Exception ie) {
          throw new ExceptionInInitializerError(ie);
        }
      }
    }

    public static native long openPmemRoot(String path, long size);
    public static native void freePmemRoot(long addr, long size);
    //TODO closePmemRoot from only path
}
