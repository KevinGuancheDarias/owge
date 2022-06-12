package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.entity.EntityWithRelation;
import com.kevinguanchedarias.owgejava.entity.EntityWithRelationImp;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PreRemove;

/**
 * Handles the fetching, saving, and deleting of entities that supports
 * ObjectRelation connection
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Component
@Lazy
public class EntityWithRelationListener {

    private final ObjectRelationBo objectRelationBo;

    @Lazy
    public EntityWithRelationListener(ObjectRelationBo objectRelationBo) {
        super();
        this.objectRelationBo = objectRelationBo;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * <p>
     * To understand the REQUIRES_NEW see:
     * * https://stackoverflow.com/a/62539018/1922558
     */
    @PostLoad
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void defineRelation(EntityWithRelation entityWithRelation) {
        entityWithRelation.setRelation(objectRelationBo
                .findOne(entityWithRelation.getObject(), entityWithRelation.getId()));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostPersist
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveRelation(EntityWithRelation entityWithRelation) {
        entityWithRelation
                .setRelation(objectRelationBo.create(entityWithRelation.getObject(), entityWithRelation.getId()));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PreRemove
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeRelation(EntityWithRelationImp entityWithRelation) {
        ObjectRelation relation = entityWithRelation.getRelation();
        objectRelationBo.delete(relation);
    }
}
