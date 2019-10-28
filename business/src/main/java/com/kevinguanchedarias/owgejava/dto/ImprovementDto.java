package com.kevinguanchedarias.owgejava.dto;

import java.util.ArrayList;
import java.util.List;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.UserImprovement;

public class ImprovementDto implements DtoFromEntity<Improvement> {
	private Integer id;
	private Float moreSoldiersProduction;
	private Float morePrimaryResourceProduction;
	private Float moreSecondaryResourceProduction;
	private Float moreEnergyProduction;
	private Float moreChargeCapacity;
	private Float moreMisions;
	private Float moreUpgradeResearchSpeed;
	private Float moreUnitBuildSpeed;
	private List<ImprovementUnitTypeDto> unitTypesUpgrades;

	@Override
	public void dtoFromEntity(Improvement entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
		unitTypesUpgrades = new ArrayList<>();
		if (entity.getUnitTypesUpgrades() != null) {
			entity.getUnitTypesUpgrades().forEach(current -> {
				ImprovementUnitTypeDto currentDto = new ImprovementUnitTypeDto();
				currentDto.dtoFromEntity(current);
			});
		}
	}

	public void dtoFromEntity(UserImprovement entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Float getMoreSoldiersProduction() {
		return moreSoldiersProduction;
	}

	public void setMoreSoldiersProduction(Float moreSoldiersProduction) {
		this.moreSoldiersProduction = moreSoldiersProduction;
	}

	public Float getMorePrimaryResourceProduction() {
		return morePrimaryResourceProduction;
	}

	public void setMorePrimaryResourceProduction(Float morePrimaryResourceProduction) {
		this.morePrimaryResourceProduction = morePrimaryResourceProduction;
	}

	public Float getMoreSecondaryResourceProduction() {
		return moreSecondaryResourceProduction;
	}

	public void setMoreSecondaryResourceProduction(Float moreSecondaryResourceProduction) {
		this.moreSecondaryResourceProduction = moreSecondaryResourceProduction;
	}

	public Float getMoreEnergyProduction() {
		return moreEnergyProduction;
	}

	public void setMoreEnergyProduction(Float moreEnergyProduction) {
		this.moreEnergyProduction = moreEnergyProduction;
	}

	public Float getMoreChargeCapacity() {
		return moreChargeCapacity;
	}

	public void setMoreChargeCapacity(Float moreChargeCapacity) {
		this.moreChargeCapacity = moreChargeCapacity;
	}

	public Float getMoreMisions() {
		return moreMisions;
	}

	public void setMoreMisions(Float moreMisions) {
		this.moreMisions = moreMisions;
	}

	/**
	 * @return the moreUpgradeResearchSpeed
	 */
	public Float getMoreUpgradeResearchSpeed() {
		return moreUpgradeResearchSpeed;
	}

	/**
	 * @param moreUpgradeResearchSpeed the moreUpgradeResearchSpeed to set
	 */
	public void setMoreUpgradeResearchSpeed(Float moreUpgradeResearchSpeed) {
		this.moreUpgradeResearchSpeed = moreUpgradeResearchSpeed;
	}

	/**
	 * @return the moreUnitBuildSpeed
	 */
	public Float getMoreUnitBuildSpeed() {
		return moreUnitBuildSpeed;
	}

	/**
	 * @param moreUnitBuildSpeed the moreUnitBuildSpeed to set
	 */
	public void setMoreUnitBuildSpeed(Float moreUnitBuildSpeed) {
		this.moreUnitBuildSpeed = moreUnitBuildSpeed;
	}

	public List<ImprovementUnitTypeDto> getUnitTypesUpgrades() {
		return unitTypesUpgrades;
	}

	public void setUnitTypesUpgrades(List<ImprovementUnitTypeDto> unitTypesUpgrades) {
		this.unitTypesUpgrades = unitTypesUpgrades;
	}

}
