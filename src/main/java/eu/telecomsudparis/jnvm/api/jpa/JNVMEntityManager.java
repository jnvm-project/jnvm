package eu.telecomsudparis.jnvm.api.jpa;

import java.util.Map;
import java.util.List;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.PersistenceException;
import javax.persistence.SynchronizationType;
import javax.persistence.LockModeType;
import javax.persistence.FlushModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.EntityTransaction;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;

public class JNVMEntityManager implements EntityManager {
    private EntityManagerFactory emf;
    private SynchronizationType syncType;
    private boolean closed = false;

    public JNVMEntityManager(EntityManagerFactory emf, SynchronizationType syncType) {
        this.emf = emf;
        this.syncType = syncType;
    }

    @Override
    public void persist(Object entity) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("persist");
    }
    @Override
    public <T> T merge(T entity) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("merge");
    }
    @Override
    public void remove(Object entity) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("remove");
    }
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("find");
    }
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      Map<String, Object> properties) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("find");
    }
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      LockModeType lockMode) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("find");
    }
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      LockModeType lockMode,
                      Map<String, Object> properties) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("find");
    }
    @Override
    public <T> T getReference(Class<T> entityClass,
                              Object primaryKey) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("getReference");
    }
    @Override
    public void flush() {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("flush");
    }
    @Override
    public void setFlushMode(FlushModeType flushMode) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("setFlushMode");
    }
    @Override
    public FlushModeType getFlushMode() {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("getFlushMode");
    }
    @Override
    public void lock(Object entity, LockModeType lockMode) {
        assertIsOpen();

        throw new UnsupportedOperationException("lock");
    }
    @Override
    public void lock(Object entity, LockModeType lockMode,
                     Map<String, Object> properties) {
        assertIsOpen();

        throw new UnsupportedOperationException("lock");
    }
    @Override
    public void refresh(Object entity) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("refresh");
    }
    @Override
    public void refresh(Object entity,
                        Map<String, Object> properties) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("refresh");
    }
    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("refresh");
    }
    @Override
    public void refresh(Object entity, LockModeType lockMode,
                        Map<String, Object> properties) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("refresh");
    }
    @Override
    public void clear() {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("clear");
    }
    @Override
    public void detach(Object entity) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("detach");
    }
    @Override
    public boolean contains(Object entity) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("contains");
    }
    @Override
    public LockModeType getLockMode(Object entity) {
        assertIsOpen();

        throw new UnsupportedOperationException("getLockMode");
    }
    @Override
    public void setProperty(String propertyName, Object value) {
        assertIsOpen();

        throw new UnsupportedOperationException("setProperty");
    }
    @Override
    public Map<String, Object> getProperties() {
        throw new UnsupportedOperationException("getProperties");
    }
    @Override
    public Query createQuery(String qlString) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public Query createNamedQuery(String name) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNamedQuery");
    }
    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNamedQuery");
    }
    @Override
    public Query createNativeQuery(String sqlString) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNativeQuery");
    }
    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNativeQuery");
    }
    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNativeQuery");
    }
    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNamedStoredProcedureQuery");
    }
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createStoredProcedureQuery");
    }
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, Class... resultClasses) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createStoredProcedureQuery");
    }
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, String... resultSetMappings) {
        assertIsOpen();

        //No plans for querying support yet!
        throw new UnsupportedOperationException("createStoredProcedureQuery");
    }
    @Override
    public void joinTransaction() {
        assertIsOpen();

        //No plans for transaction support yet!
        throw new UnsupportedOperationException("joinTransaction");
    }
    @Override
    public boolean isJoinedToTransaction() {
        assertIsOpen();

        //No plans for transaction support yet!
        throw new UnsupportedOperationException("isJoinedToTransaction");
    }
    @Override
    public <T> T unwrap(Class<T> cls) {
        assertIsOpen();

        throw new PersistenceException("Not yet supported unwrapping of " + cls.getName());
    }
    @Override
    public Object getDelegate() {
        assertIsOpen();

        throw new UnsupportedOperationException("getDelegate");
    }
    @Override
    public void close() {
        assertIsOpen();

        //TODO All holded resources should be released here once
        //TODO Keep persistence context managed until ongoing transaction completes
        closed = true;
    }
    @Override
    public boolean isOpen() {
        return !closed;
    }
    @Override
    public EntityTransaction getTransaction() {
        //No plans for transaction support yet!
        throw new UnsupportedOperationException("getTransaction");
    }
    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        assertIsOpen();

        return emf;
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
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("createEntityGraph");
    }
    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("createEntityGraph");
    }
    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("getEntityGraph");
    }
    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        assertIsOpen();

        //TODO Implement me !
        throw new UnsupportedOperationException("getEntityGraphs");
    }

    private void assertIsOpen() {
        if(closed)
            throw new IllegalStateException("EntityManager is closed");
    }
}
