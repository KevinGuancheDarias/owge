package com.kevinguanchedarias.owgejava.entity.listener;

import javax.persistence.PostLoad;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Lazy
@Component
public class RequirementGroupListener {

	private final RequirementBo requirementBo;

	@Lazy
	public RequirementGroupListener(RequirementBo requirementBo) {
		this.requirementBo = requirementBo;
	}

	/**
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostLoad
	public void computeRequirements(RequirementGroup requirementGroup) {
		requirementGroup.setRequirements(
				requirementBo.findRequirements(ObjectEnum.REQUIREMENT_GROUP, requirementGroup.getId()));
	}
}
