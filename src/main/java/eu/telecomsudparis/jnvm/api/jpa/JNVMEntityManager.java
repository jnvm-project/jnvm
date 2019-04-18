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

    public JNVMEntityManager(EntityManagerFactory emf, SynchronizationType syncType) {
        this.emf = emf;
        this.syncType = syncType;
    }

    @Override
    public void persist(Object entity) {
        //TODO Implement me !
        throw new UnsupportedOperationException("persist");
    }
    @Override
    public <T> T merge(T entity) {
        //TODO Implement me !
        throw new UnsupportedOperationException("merge");
    }
    @Override
    public void remove(Object entity) {
        //TODO Implement me !
        throw new UnsupportedOperationException("remove");
    }
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        //TODO Implement me !
        throw new UnsupportedOperationException("find");
    }
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      Map<String, Object> properties) {
        //TODO Implement me !
        throw new UnsupportedOperationException("find");
    }
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      LockModeType lockMode) {
        //TODO Implement me !
        throw new UnsupportedOperationException("find");
    }
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      LockModeType lockMode,
                      Map<String, Object> properties) {
        //TODO Implement me !
        throw new UnsupportedOperationException("find");
    }
    @Override
    public <T> T getReference(Class<T> entityClass,
                              Object primaryKey) {
        //TODO Implement me !
        throw new UnsupportedOperationException("getReference");
    }
    @Override
    public void flush() {
        //TODO Implement me !
        throw new UnsupportedOperationException("flush");
    }
    @Override
    public void setFlushMode(FlushModeType flushMode) {
        //TODO Implement me !
        throw new UnsupportedOperationException("setFlushMode");
    }
    @Override
    public FlushModeType getFlushMode() {
        //TODO Implement me !
        throw new UnsupportedOperationException("getFlushMode");
    }
    @Override
    public void lock(Object entity, LockModeType lockMode) {
        throw new UnsupportedOperationException("lock");
    }
    @Override
    public void lock(Object entity, LockModeType lockMode,
                     Map<String, Object> properties) {
        throw new UnsupportedOperationException("lock");
    }
    @Override
    public void refresh(Object entity) {
        //TODO Implement me !
        throw new UnsupportedOperationException("refresh");
    }
    @Override
    public void refresh(Object entity,
                        Map<String, Object> properties) {
        //TODO Implement me !
        throw new UnsupportedOperationException("refresh");
    }
    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        //TODO Implement me !
        throw new UnsupportedOperationException("refresh");
    }
    @Override
    public void refresh(Object entity, LockModeType lockMode,
                        Map<String, Object> properties) {
        //TODO Implement me !
        throw new UnsupportedOperationException("refresh");
    }
    @Override
    public void clear() {
        //TODO Implement me !
        throw new UnsupportedOperationException("clear");
    }
    @Override
    public void detach(Object entity) {
        //TODO Implement me !
        throw new UnsupportedOperationException("detach");
    }
    @Override
    public boolean contains(Object entity) {
        //TODO Implement me !
        throw new UnsupportedOperationException("contains");
    }
    @Override
    public LockModeType getLockMode(Object entity) {
        throw new UnsupportedOperationException("getLockMode");
    }
    @Override
    public void setProperty(String propertyName, Object value) {
        throw new UnsupportedOperationException("setProperty");
    }
    @Override
    public Map<String, Object> getProperties() {
        throw new UnsupportedOperationException("getProperties");
    }
    @Override
    public Query createQuery(String qlString) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createQuery");
    }
    @Override
    public Query createNamedQuery(String name) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNamedQuery");
    }
    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNamedQuery");
    }
    @Override
    public Query createNativeQuery(String sqlString) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNativeQuery");
    }
    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNativeQuery");
    }
    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNativeQuery");
    }
    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createNamedStoredProcedureQuery");
    }
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createStoredProcedureQuery");
    }
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, Class... resultClasses) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createStoredProcedureQuery");
    }
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, String... resultSetMappings) {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("createStoredProcedureQuery");
    }
    @Override
    public void joinTransaction() {
        //No plans for transaction support yet!
        throw new UnsupportedOperationException("joinTransaction");
    }
    @Override
    public boolean isJoinedToTransaction() {
        //No plans for transaction support yet!
        throw new UnsupportedOperationException("isJoinedToTransaction");
    }
    @Override
    public <T> T unwrap(Class<T> cls) {
        throw new PersistenceException("Not yet supported unwrapping of " + cls.getName());
    }
    @Override
    public Object getDelegate() {
        throw new UnsupportedOperationException("getDelegate");
    }
    @Override
    public void close() {
        //TODO Implement me !
        throw new UnsupportedOperationException("close");
    }
    @Override
    public boolean isOpen() {
        //TODO Implement me !
        throw new UnsupportedOperationException("isOpen");
    }
    @Override
    public EntityTransaction getTransaction() {
        //No plans for transaction support yet!
        throw new UnsupportedOperationException("getTransaction");
    }
    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        //No plans for querying support yet!
        throw new UnsupportedOperationException("getCriteriaBuilder");
    }
    @Override
    public Metamodel getMetamodel() {
        //TODO Implement me !
        throw new UnsupportedOperationException("getMetamodel");
    }
    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        //TODO Implement me !
        throw new UnsupportedOperationException("createEntityGraph");
    }
    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        //TODO Implement me !
        throw new UnsupportedOperationException("createEntityGraph");
    }
    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        //TODO Implement me !
        throw new UnsupportedOperationException("getEntityGraph");
    }
    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        //TODO Implement me !
        throw new UnsupportedOperationException("getEntityGraphs");
    }
}
