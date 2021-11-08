package eu.telecomsudparis.jnvm.jpa;

import java.util.Date;
import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SampleObject<V> implements Serializable {
    @Id
    private long key;
    @Basic
    private V value;
    @Basic
    private Date createdAt = new Date();
    @Basic
    private Date lastModifiedAt;

    SampleObject(long key, V value) {
        this.key = key;
        this.value = value;
        this.lastModifiedAt = createdAt;
    }

    public long getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setValue(V value) {
        this.value = value;
        this.lastModifiedAt = new Date();
    }

    @Override
    public boolean equals(Object a) {
        if( this == a )
            return true;
        else if( a == null )
            return false;
        else if( this.getClass() != a.getClass() )
            return false;
        else {
            final SampleObject<V> o = (SampleObject<V>) a;
            return key == o.getKey()
                && value.equals(o.getValue())
                && createdAt.equals(o.getCreatedAt())
                && lastModifiedAt.equals(o.getLastModifiedAt());
        }
    }
}
