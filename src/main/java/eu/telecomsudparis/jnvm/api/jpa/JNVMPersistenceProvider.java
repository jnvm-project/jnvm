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
        return new JNVMEntityManagerFactory(emName, properties);
    }
    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo emInfo, Map properties) {
        return new JNVMEntityManagerFactory(emInfo, properties);
    }
    @Override
    public void generateSchema(PersistenceUnitInfo emInfo, Map properties) {
    }
    @Override
    public boolean generateSchema(String emName, Map properties) {
        return false;
    }
    @Override
    public ProviderUtil getProviderUtil() {
        return new ProviderUtilImpl();
    }
    private static class ProviderUtilImpl implements ProviderUtil {
        @Override
        public LoadState isLoadedWithoutReference(Object entity, String attributeName) {
            return LoadState.NOT_LOADED;
        }
        @Override
        public LoadState isLoadedWithReference(Object entity, String attributeName) {
            return LoadState.NOT_LOADED;
        }
        @Override
        public LoadState isLoaded(Object entity) {
            return LoadState.NOT_LOADED;
        }
    }
}
