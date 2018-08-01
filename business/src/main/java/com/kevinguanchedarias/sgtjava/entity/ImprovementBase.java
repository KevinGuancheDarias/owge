package com.kevinguanchedarias.sgtjava.entity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@MappedSuperclass
public abstract class ImprovementBase implements SimpleIdEntity {
	private static final long serialVersionUID = 8483043984040996933L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	/**
	 * @deprecated Unused property, the system uses now an UnitType requirement
	 *             of type AMOUNT
	 */
	@Deprecated
	@Column(name = "more_soldiers_production")
	private Float moreSoldiersProduction = 0.0F;

	@Column(name = "more_primary_resource_production")
	private Float morePrimaryResourceProduction = 0.0F;

	@Column(name = "more_secondary_resource_production")
	private Float moreSecondaryResourceProduction = 0.0F;

	@Column(name = "more_energy_production")
	private Float moreEnergyProduction = 0.0F;

	@Column(name = "more_charge_capacity")
	private Float moreChargeCapacity = 0.0F;

	@Column(name = "more_missions_value")
	private Float moreMisions = 0.0F;

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @deprecated Unused property, the system uses now an UnitType requirement
	 *             of type AMOUNT
	 * 
	 * @param value
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated
	public void addMoreSoldiersProduction(Float value) {
		moreSoldiersProduction += value;
	}

	public void addMorePrimaryResourceProduction(Float value) {
		morePrimaryResourceProduction += value;
	}

	public void addMoreSecondaryResourceProduction(Float value) {
		moreSecondaryResourceProduction += value;
	}

	public void addMoreEnergyProduction(Float value) {
		moreEnergyProduction += value;
	}

	public void addMoreChargeCapacity(Float value) {
		moreChargeCapacity += value;
	}

	public void addMoreMissions(Float value) {
		moreMisions += value;
	}

	public Double findRationalChargeCapacity() {
		return moreChargeCapacity / (double) 100;
	}

	/**
	 * @deprecated Unused property, the system uses now an UnitType requirement
	 *             of type AMOUNT
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated
	public Float getMoreSoldiersProduction() {
		return moreSoldiersProduction;
	}

	/**
	 * @deprecated Unused property, the system uses now an UnitType requirement
	 *             of type AMOUNT
	 * @param moreSoldiersProduction
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated
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
}
