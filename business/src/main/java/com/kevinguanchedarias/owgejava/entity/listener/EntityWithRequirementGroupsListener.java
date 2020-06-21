package com.kevinguanchedarias.owgejava.entity.listener;

import java.util.stream.Collectors;

import javax.persistence.PostLoad;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.ObjectRelationToObjectRelationBo;
import com.kevinguanchedarias.owgejava.entity.EntityWithRequirementGroups;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Component
public class EntityWithRequirementGroupsListener {
	private ObjectRelationBo objectRelationBo;
	private ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo;

	@Lazy
	public EntityWithRequirementGroupsListener(ObjectRelationBo objectRelationBo,
			ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo) {
		this.objectRelationBo = objectRelationBo;
		this.objectRelationToObjectRelationBo = objectRelationToObjectRelationBo;
	}

	/**
	 *
	 * @param entityWithGroupRequirements
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostLoad
	public void loadRequirements(EntityWithRequirementGroups entityWithGroupRequirements) {
		entityWithGroupRequirements
				.setRequirementGroups(
						objectRelationToObjectRelationBo
								.findByMasterId(entityWithGroupRequirements.getRelation().getId()).stream()
								.map(relationToRelation -> (RequirementGroup) objectRelationBo
										.unboxObjectRelation(relationToRelation.getSlave()))
								.collect(Collectors.toList()));
	}
}
