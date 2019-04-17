package eu.telecomsudparis.jnvm.api.jpa;

import java.util.Map;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.SynchronizationType;
import javax.persistence.Cache;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class JNVMEntityManagerFactory implements EntityManagerFactory {
    public JNVMEntityManagerFactory(String emName, Map properties) {
    }
    public JNVMEntityManagerFactory(PersistenceUnitInfo emInfo, Map properties) {
    }

    @Override
    public EntityManager createEntityManager() {
        return createEntityManager(SynchronizationType.SYNCHRONIZED, null);
    }
    @Override
    public EntityManager createEntityManager(Map map) {
        return createEntityManager(SynchronizationType.SYNCHRONIZED, map);
    }
    @Override
    public EntityManager createEntityManager(SynchronizationType syncType) {
        return createEntityManager(syncType, null);
    }
    @Override
    public EntityManager createEntityManager(SynchronizationType syncType, Map map) {
        return new JNVMEntityManager(this, syncType);
    }
    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return null;
    }
    @Override
    public Metamodel getMetamodel() {
        return null;
    }
    @Override
    public boolean isOpen() {
        return false;
    }
    @Override
    public void close() {
    }
    @Override
    public Map<String, Object> getProperties() {
        return null;
    }
    @Override
    public Cache getCache() {
        return null;
    }
    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return null;
    }
    @Override
    public void addNamedQuery(String name, Query query) {
    }
    @Override
    public <T> T unwrap(Class<T> cls) {
        return null;
    }
    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
    }
}
