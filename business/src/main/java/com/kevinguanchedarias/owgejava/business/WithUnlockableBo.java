package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;

import java.io.Serializable;
import java.util.List;

/**
 * Allows to request currently unlocked entities to child requesters
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
public interface WithUnlockableBo<K extends Serializable, E extends EntityWithId<K>, D extends DtoFromEntity<E>>
        extends WithToDtoTrait<E, D> {
    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    ObjectEnum getObject();

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    UnlockedRelationBo getUnlockedRelationBo();

    /**
     * Finds the unlocked entities for given user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    default List<E> findUnlocked(Integer userId) {
        return getUnlockedRelationBo()
                .unboxToTargetEntity(getUnlockedRelationBo().findByUserIdAndObjectType(userId, getObject()));
    }

    /**
     * Finds the unlocked entities for given user
     * .9.0
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    default List<E> findUnlocked(UserStorage user) {
        return findUnlocked(user.getId());
    }
}
