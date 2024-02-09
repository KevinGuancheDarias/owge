package com.kevinguanchedarias.owgejava.entity.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObjectRelationToObjectRelation;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
public class ObjectRelationToObjectRelationListener {

    /**
     * @param objectRelationToObjectRelation
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PrePersist
    @PreUpdate
    @Transactional(propagation = Propagation.MANDATORY)
    public void validateProgrammingSmell(ObjectRelationToObjectRelation objectRelationToObjectRelation) {
        ObjectRelation masterObjectRelation = objectRelationToObjectRelation.getMaster();
        ObjectRelation slave = objectRelationToObjectRelation.getSlave();
        if (masterObjectRelation.getObject().findCodeAsEnum() == ObjectEnum.REQUIREMENT_GROUP) {
            throw new ProgrammingException("Can't use REQUIREMENT_GROUP as master");
        }
        if (slave.getObject().findCodeAsEnum() != ObjectEnum.REQUIREMENT_GROUP) {
            throw new ProgrammingException("Only REQUIREMENT_GROUP can be set as slave");
        }
    }

}
