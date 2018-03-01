package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.dao.RequirementInformationDao;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.sgtjava.entity.Upgrade;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.sgtjava.repository.UpgradeRepository;

@Component
public class UpgradeBo implements WithNameBo<Upgrade> {
	private static final long serialVersionUID = -4559943498112928568L;

	@Autowired
	private UpgradeRepository upgradeRepository;

	@Autowired
	private transient RequirementInformationDao requirementInformationDao;

	@Override
	public JpaRepository<Upgrade, Number> getRepository() {
		return upgradeRepository;
	}

	@Transactional
	@Override
	public void delete(Upgrade upgrade) {
		requirementInformationDao.deleteAllObjectRelations(RequirementTargetObject.UPGRADE, upgrade.getId());
		WithNameBo.super.delete(upgrade);
	}

	/**
	 * Returns the resource requirements required to level up
	 * 
	 * @param obtainedUpgrade
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public ResourceRequirementsPojo calculateRequirementsAreMet(ObtainedUpgrade obtainedUpgrade) {
		ResourceRequirementsPojo retVal = new ResourceRequirementsPojo();
		Upgrade upgradeRef = obtainedUpgrade.getUpgrade();
		retVal.setRequiredPrimary(upgradeRef.getPrimaryResource().doubleValue());
		retVal.setRequiredSecondary(upgradeRef.getSecondaryResource().doubleValue());
		retVal.setRequiredTime(upgradeRef.getTime().doubleValue());

		int nextLevel = obtainedUpgrade.getLevel() + 1;
		for (int i = 1; i < nextLevel; i++) {
			retVal.setRequiredPrimary(
					retVal.getRequiredPrimary() + (retVal.getRequiredPrimary() * upgradeRef.getLevelEffect()));
			retVal.setRequiredSecondary(
					retVal.getRequiredSecondary() + (retVal.getRequiredSecondary() * upgradeRef.getLevelEffect()));
			retVal.setRequiredTime(retVal.getRequiredTime() + (retVal.getRequiredTime() * upgradeRef.getLevelEffect()));
		}

		return retVal;
	}
}
