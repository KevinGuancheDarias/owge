package com.kevinguanchedarias.owgejava.entity.listener;

import javax.persistence.PostLoad;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Component
public class UpgradeListener {
	private RequirementBo requirementBo;

	@Lazy
	public UpgradeListener(RequirementBo requirementBo) {
		this.requirementBo = requirementBo;
	}

	@PostLoad
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void loadRequirements(Upgrade upgrade) {
		upgrade.setRequirements(requirementBo.findRequirements(ObjectEnum.UPGRADE, upgrade.getId()));
	}
}
