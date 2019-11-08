package com.kevinguanchedarias.owgejava.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.dto.UpgradeDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.owgejava.repository.UpgradeRepository;

@Component
public class UpgradeBo implements WithNameBo<Integer, Upgrade, UpgradeDto> {
	private static final long serialVersionUID = -4559943498112928568L;

	@Autowired
	private UpgradeRepository upgradeRepository;

	@Autowired
	private transient RequirementInformationDao requirementInformationDao;

	@Autowired
	private ObtainedUpgradeBo obtainedUpgradeBo;

	@Autowired
	private ImprovementBo improvementBo;

	@Override
	public JpaRepository<Upgrade, Integer> getRepository() {
		return upgradeRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<UpgradeDto> getDtoClass() {
		return UpgradeDto.class;
	}

	@Transactional
	@Override
	public void delete(Upgrade upgrade) {
		improvementBo.clearCacheEntriesIfRequired(upgrade, obtainedUpgradeBo);
		requirementInformationDao.deleteAllObjectRelations(RequirementTargetObject.UPGRADE, upgrade.getId());
		WithNameBo.super.delete(upgrade);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.business.BaseBo#save(com.kevinguanchedarias.
	 * owgejava.entity.EntityWithId)
	 */
	@Override
	public Upgrade save(Upgrade entity) {
		improvementBo.clearCacheEntriesIfRequired(entity, obtainedUpgradeBo);
		return WithNameBo.super.save(entity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#save(java.util.List)
	 */
	@Override
	public void save(List<Upgrade> entities) {
		improvementBo.clearCacheEntries(obtainedUpgradeBo);
		WithNameBo.super.save(entities);
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
