package eu.telecomsudparis.jnvm.jpa;

import java.util.Map;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.PersistenceException;
import javax.persistence.SynchronizationType;
import javax.persistence.Cache;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class JNVMEntityManagerFactory implements EntityManagerFactory {
    private boolean closed = false;

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
        assertIsOpen();

        return new JNVMEntityManager(this, syncType);
    }
    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("getCriteriaBuilder");
    }
    @Override
    public Metamodel getMetamodel() {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("getMetamodel");
    }
    @Override
    public boolean isOpen() {
        return !closed;
    }
    @Override
    public void close() {
        assertIsOpen();

        //TODO All holded resources should be released here once
        closed = true;
    }
    @Override
    public Map<String, Object> getProperties() {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("getProperties");
    }
    @Override
    public Cache getCache() {
        assertIsOpen();

        //We might never need a second-level cache!
        return null;
    }
    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        assertIsOpen();

        return new PersistenceUnitUtilImpl();
    }
    @Override
    public void addNamedQuery(String name, Query query) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("addNamedQuery");
    }
    @Override
    public <T> T unwrap(Class<T> cls) {
        assertIsOpen();

        throw new PersistenceException("Not yet supported unwrapping of " + cls.getName());
    }
    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("addNamedEntityGraph");
    }

    private void assertIsOpen() {
        if(closed)
            throw new IllegalStateException("EntityManagerFactory is closed");
    }

    private class PersistenceUnitUtilImpl implements PersistenceUnitUtil {
        @Override
        public boolean isLoaded(Object entity, String attributeName) {
            //TODO Implement me !
            throw new UnsupportedOperationException("isLoaded");
        }
        @Override
        public boolean isLoaded(Object entity) {
            //TODO Implement me !
            throw new UnsupportedOperationException("isLoaded");
        }
        @Override
        public Object getIdentifier(Object entity) {
            //TODO Implement me !
            throw new UnsupportedOperationException("getIdentifier");
        }
    }
}
