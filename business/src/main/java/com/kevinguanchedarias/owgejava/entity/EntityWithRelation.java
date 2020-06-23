package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.kevinguanchedarias.owgejava.entity.listener.EntityWithRelationListener;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;

/**
 * Represents an entity that has a direct 1to1 ObjectRelation
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@MappedSuperclass
@EntityListeners(EntityWithRelationListener.class)
public abstract class EntityWithRelation implements EntityWithId<Integer> {
	private static final long serialVersionUID = 2725936026511921120L;

	@Transient
	private ObjectRelation relation;

	/**
	 * Used to be able to handle the relation loading or creation
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public abstract ObjectEnum getObject();

	/**
	 * @return the relation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectRelation getRelation() {
		return relation;
	}

	/**
	 * @param relation the relation to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setRelation(ObjectRelation relation) {
		this.relation = relation;
	}

}
