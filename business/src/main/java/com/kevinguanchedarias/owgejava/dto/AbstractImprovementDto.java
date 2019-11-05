/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractImprovementDto {
	private Float moreSoldiersProduction;
	private Float morePrimaryResourceProduction;
	private Float moreSecondaryResourceProduction;
	private Float moreEnergyProduction;
	private Float moreChargeCapacity;
	private Float moreMisions;
	private Float moreUpgradeResearchSpeed;
	private Float moreUnitBuildSpeed;
	private List<ImprovementUnitTypeDto> unitTypesUpgrades;

	/**
	 * @return the moreSoldiersProduction
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getMoreSoldiersProduction() {
		return moreSoldiersProduction;
	}

	/**
	 * @param moreSoldiersProduction the moreSoldiersProduction to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMoreSoldiersProduction(Float moreSoldiersProduction) {
		this.moreSoldiersProduction = moreSoldiersProduction;
	}

	/**
	 * @return the morePrimaryResourceProduction
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getMorePrimaryResourceProduction() {
		return morePrimaryResourceProduction;
	}

	/**
	 * @param morePrimaryResourceProduction the morePrimaryResourceProduction to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMorePrimaryResourceProduction(Float morePrimaryResourceProduction) {
		this.morePrimaryResourceProduction = morePrimaryResourceProduction;
	}

	/**
	 * @return the moreSecondaryResourceProduction
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getMoreSecondaryResourceProduction() {
		return moreSecondaryResourceProduction;
	}

	/**
	 * @param moreSecondaryResourceProduction the moreSecondaryResourceProduction to
	 *                                        set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMoreSecondaryResourceProduction(Float moreSecondaryResourceProduction) {
		this.moreSecondaryResourceProduction = moreSecondaryResourceProduction;
	}

	/**
	 * @return the moreEnergyProduction
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getMoreEnergyProduction() {
		return moreEnergyProduction;
	}

	/**
	 * @param moreEnergyProduction the moreEnergyProduction to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMoreEnergyProduction(Float moreEnergyProduction) {
		this.moreEnergyProduction = moreEnergyProduction;
	}

	/**
	 * @return the moreChargeCapacity
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getMoreChargeCapacity() {
		return moreChargeCapacity;
	}

	/**
	 * @param moreChargeCapacity the moreChargeCapacity to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMoreChargeCapacity(Float moreChargeCapacity) {
		this.moreChargeCapacity = moreChargeCapacity;
	}

	/**
	 * @return the moreMisions
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getMoreMisions() {
		return moreMisions;
	}

	/**
	 * @param moreMisions the moreMisions to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMoreMisions(Float moreMisions) {
		this.moreMisions = moreMisions;
	}

	/**
	 * @return the moreUpgradeResearchSpeed
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getMoreUpgradeResearchSpeed() {
		return moreUpgradeResearchSpeed;
	}

	/**
	 * @param moreUpgradeResearchSpeed the moreUpgradeResearchSpeed to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMoreUpgradeResearchSpeed(Float moreUpgradeResearchSpeed) {
		this.moreUpgradeResearchSpeed = moreUpgradeResearchSpeed;
	}

	/**
	 * @return the moreUnitBuildSpeed
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Float getMoreUnitBuildSpeed() {
		return moreUnitBuildSpeed;
	}

	/**
	 * @param moreUnitBuildSpeed the moreUnitBuildSpeed to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMoreUnitBuildSpeed(Float moreUnitBuildSpeed) {
		this.moreUnitBuildSpeed = moreUnitBuildSpeed;
	}

	/**
	 * @return the unitTypesUpgrades
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ImprovementUnitTypeDto> getUnitTypesUpgrades() {
		return unitTypesUpgrades;
	}

	/**
	 * @param unitTypesUpgrades the unitTypesUpgrades to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setUnitTypesUpgrades(List<ImprovementUnitTypeDto> unitTypesUpgrades) {
		this.unitTypesUpgrades = unitTypesUpgrades;
	}

	public AbstractImprovementDto addMorePrimaryResourceProduction(Float value) {
		morePrimaryResourceProduction += value;
		return this;
	}

	public AbstractImprovementDto addMoreSecondaryResourceProduction(Float value) {
		moreSecondaryResourceProduction += value;
		return this;
	}

	public AbstractImprovementDto addMoreEnergyProduction(Float value) {
		moreEnergyProduction += value;
		return this;
	}

	public AbstractImprovementDto addMoreChargeCapacity(Float value) {
		moreChargeCapacity += value;
		return this;
	}

	public AbstractImprovementDto addMoreMissions(Float value) {
		moreMisions += value;
		return this;
	}

	public AbstractImprovementDto addMoreUpgradeResearchSpeed(Float value) {
		moreUpgradeResearchSpeed += value;
		return this;
	}

	public AbstractImprovementDto addMoreUnitBuildSpeed(Float value) {
		moreUnitBuildSpeed += value;
		return this;
	}

	/**
	 * Initializes all the values to zero
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	protected void initToZeroes() {
		morePrimaryResourceProduction = 0F;
		moreSecondaryResourceProduction = 0F;
		moreEnergyProduction = 0F;
		moreChargeCapacity = 0F;
		moreMisions = 0F;
		moreUpgradeResearchSpeed = 0F;
		moreUnitBuildSpeed = 0F;
		unitTypesUpgrades = new ArrayList<>();
	}
}
