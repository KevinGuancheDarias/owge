package com.kevinguanchedarias.owgejava.entity.listener;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PreRemove;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.RequirementInformationBo;
import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.entity.EntityWithRelation;
import com.kevinguanchedarias.owgejava.entity.EntityWithRelationImp;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;

/**
 * Handles the fetching, saving, and deleting of entities that supports
 * ObjectRelation connection
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Component
@Lazy
public class EntityWithRelationListener {

	private ObjectRelationBo objectRelationBo;
	private RequirementInformationBo requirementInformationBo;
	private UnlockedRelationBo unlockedRelationBo;

	@Lazy
	public EntityWithRelationListener(ObjectRelationBo objectRelationBo,
			RequirementInformationBo requirementInformationBo, UnlockedRelationBo unlockedRelationBo) {
		super();
		this.objectRelationBo = objectRelationBo;
		this.requirementInformationBo = requirementInformationBo;
		this.unlockedRelationBo = unlockedRelationBo;
	}

	/**
	 *
	 * @param entityWithRelation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @see To understand the REQUIRES_NEW see:
	 *      https://stackoverflow.com/a/62539018/1922558
	 */
	@PostLoad
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void defineRelation(EntityWithRelation entityWithRelation) {
		entityWithRelation.setRelation(objectRelationBo
				.findOneByObjectTypeAndReferenceId(entityWithRelation.getObject(), entityWithRelation.getId()));
	}

	/**
	 *
	 * @param entityWithRelation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostPersist
	@Transactional(propagation = Propagation.MANDATORY)
	public void saveRelation(EntityWithRelation entityWithRelation) {
		entityWithRelation
				.setRelation(objectRelationBo.create(entityWithRelation.getObject(), entityWithRelation.getId()));
	}

	/**
	 *
	 * @param entityWithRelation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PreRemove
	@Transactional(propagation = Propagation.MANDATORY)
	public void removeRelation(EntityWithRelationImp entityWithRelation) {
		ObjectRelation relation = entityWithRelation.getRelation();
		requirementInformationBo.deleteByRelation(relation);
		unlockedRelationBo.deleteByRelation(relation);
		objectRelationBo.delete(relation);
	}
}
