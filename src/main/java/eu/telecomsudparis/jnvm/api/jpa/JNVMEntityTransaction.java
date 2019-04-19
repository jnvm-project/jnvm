package eu.telecomsudparis.jnvm.api.jpa;

import javax.persistence.EntityTransaction;

public class JNVMEntityTransaction implements EntityTransaction {
    private boolean active = false;
    private boolean rollbackOnly = false;

    @Override
    public void begin() {
        assertInactive();

        active = true;
        //TODO Add begin TX code
    }
    @Override
    public void commit() {
        assertActive();

        //TODO Add commit TX code (em flush)
        //TODO throw RollbackException upon commit failure
    }
    @Override
    public void rollback() {
        assertActive();

        //TODO Add rollback TX code
        //TODO throw PersistenceException upon unexpected error condition
    }
    @Override
    public void setRollbackOnly() {
        assertActive();

        rollbackOnly = true;
    }
    @Override
    public boolean getRollbackOnly() {
        assertActive();

        return rollbackOnly;
    }
    @Override
    public boolean isActive() {
        //TODO throw PersistenceException upon unexpected error condition

        return active;
    }

    public void assertInactive() {
        if(active)
            throw new IllegalStateException("015032");
    }

    public void assertActive() {
        if(!active)
            throw new IllegalStateException("015040");
    }
}
