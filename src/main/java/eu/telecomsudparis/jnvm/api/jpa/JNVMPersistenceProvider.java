package eu.telecomsudparis.jnvm.api.jpa;

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.LoadState;

public class JNVMPersistenceProvider implements PersistenceProvider {
    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map properties) {
        //TODO Validate parameters
        return new JNVMEntityManagerFactory(emName, properties);
    }
    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo emInfo, Map properties) {
        //TODO Validate parameters
        return new JNVMEntityManagerFactory(emInfo, properties);
    }
    @Override
    public void generateSchema(PersistenceUnitInfo emInfo, Map properties) {
        throw new UnsupportedOperationException("generateSchema");
    }
    @Override
    public boolean generateSchema(String emName, Map properties) {
        throw new UnsupportedOperationException("generateSchema");
    }
    @Override
    public ProviderUtil getProviderUtil() {
        return new ProviderUtilImpl();
    }
    private class ProviderUtilImpl implements ProviderUtil {
        @Override
        public LoadState isLoadedWithoutReference(Object entity, String attributeName) {
            //TODO Implement me !
            throw new UnsupportedOperationException("isLoadedWithoutReference");
        }
        @Override
        public LoadState isLoadedWithReference(Object entity, String attributeName) {
            //TODO Implement me !
            throw new UnsupportedOperationException("isLoadedWithReference");
        }
        @Override
        public LoadState isLoaded(Object entity) {
            //TODO Implement me !
            throw new UnsupportedOperationException("isLoaded");
        }
    }
}
