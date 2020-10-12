package eu.telecomsudparis.jnvm.config;

import java.io.InputStream;
import java.util.Properties;


public class Environment {

    public static final String JNVM_HEAP_PATH = "jnvm.heap.path";
    public static final String JNVM_HEAP_SIZE = "jnvm.heap.size";
    private static Properties properties;

    private static void loadProperties() {
        properties = new Properties();
        //Load from config file
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream stream = cl.getResourceAsStream("jnvm.properties");
            if( stream != null ) {
                properties.load( stream );
                stream.close();
            }
        } catch( Exception e ) {
            throw new ExceptionInInitializerError( e );
        }
        //Load from system properties
        String heapPath; String heapSize;
        if(( heapPath = System.getProperty( JNVM_HEAP_PATH )) != null)
            properties.setProperty(JNVM_HEAP_PATH, heapPath);
        if(( heapSize = System.getProperty( JNVM_HEAP_SIZE )) != null)
            properties.setProperty(JNVM_HEAP_SIZE, heapSize);
    }

    private Environment() {
        throw new UnsupportedOperationException();
    }

    public static Properties getProperties() {
        if( properties == null ) loadProperties();
        return properties;
    }

}
