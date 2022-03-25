package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.EntityWithRelationListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.io.Serial;

/**
 * Represents an entity that has a direct 1to1 ObjectRelation
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@MappedSuperclass
@EntityListeners(EntityWithRelationListener.class)
public abstract class EntityWithRelationImp implements EntityWithRelation {
    @Serial
    private static final long serialVersionUID = 2725936026511921120L;

    @Transient
    private ObjectRelation relation;

    /**
     * @return the relation
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Override
    public ObjectRelation getRelation() {
        return relation;
    }

    /**
     * @param relation the relation to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    @Override
    public void setRelation(ObjectRelation relation) {
        this.relation = relation;
    }

}
