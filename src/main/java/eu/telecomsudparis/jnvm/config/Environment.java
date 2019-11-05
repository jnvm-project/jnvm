package eu.telecomsudparis.jnvm.config;

import java.io.InputStream;
import java.util.Properties;


public class Environment {

    private static Properties properties;

    private static void loadProperties() {
        properties = new Properties();
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream stream = cl.getResourceAsStream("jnvm.properties");
            properties.load( stream );
            stream.close();
        } catch( Exception e ) {
            throw new ExceptionInInitializerError( e );
        }
    }

    private Environment() {
        throw new UnsupportedOperationException();
    }

    public static Properties getProperties() {
        if( properties == null ) loadProperties();
        return properties;
    }

}
