package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;
import java.util.List;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;

/**
 * Allows to request currently unlocked entities to child requesters
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface WithUnlockableBo<K extends Serializable, E extends EntityWithId<K>, D extends DtoFromEntity<E>>
		extends BaseBo<K, E, D> {
	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	ObjectEnum getObject();

	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	UnlockedRelationBo getUnlockedRelationBo();

	/**
	 * Finds the unlocked entities for given user
	 *
	 * @param userId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public default List<E> findUnlocked(Integer userId) {
		return getUnlockedRelationBo()
				.unboxToTargetEntity(getUnlockedRelationBo().findByUserIdAndObjectType(userId, getObject()));
	}

	/**
	 * Finds the unlocked entities for given user
	 *
	 * @param user
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public default List<E> findUnlocked(UserStorage user) {
		return findUnlocked(user.getId());
	}
}
