package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;

/**
 * Represents a speed group, the special conditions to move units <br>
 * Notice: The requirements mean the unit having this speed group will be able to cross galaxies
 * 
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "speed_impact_groups")
public class SpeedImpactGroup extends EntityWithRequirementGroups {
	private static final long serialVersionUID = 8120163868349636675L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 50)
	private String name;

	@Column(name = "is_fixed")
	private Boolean isFixed = false;

	@Column(name = "mission_explore", nullable = false)
	private Double missionExplore = 0D;

	@Column(name = "mission_gather", nullable = false)
	private Double missionGather = 0D;

	@Column(name = "mission_establish_base", nullable = false)
	private Double missionEstablishBase = 0D;

	@Column(name = "mission_attack", nullable = false)
	private Double missionAttack = 0D;

	@Column(name = "mission_conquest", nullable = false)
	private Double missionConquest = 0D;

	@Column(name = "mission_counterattack", nullable = false)
	private Double missionCounterattack = 0D;

	@Transient
	private transient List<RequirementGroup> requirementGroups;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * When is fixed, the speed is not affected by the distance
	 *
	 * @return the isFixed
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getIsFixed() {
		return isFixed;
	}

	/**
	 * @param isFixed the isFixed to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setIsFixed(Boolean isFixed) {
		this.isFixed = isFixed;
	}

	public Double getMissionExplore() {
		return missionExplore;
	}

	public void setMissionExplore(Double missionExplore) {
		this.missionExplore = missionExplore;
	}

	public Double getMissionGather() {
		return missionGather;
	}

	public void setMissionGather(Double missionGather) {
		this.missionGather = missionGather;
	}

	public Double getMissionEstablishBase() {
		return missionEstablishBase;
	}

	public void setMissionEstablishBase(Double missionEstablishBase) {
		this.missionEstablishBase = missionEstablishBase;
	}

	public Double getMissionAttack() {
		return missionAttack;
	}

	public void setMissionAttack(Double missionAttack) {
		this.missionAttack = missionAttack;
	}

	public Double getMissionConquest() {
		return missionConquest;
	}

	public void setMissionConquest(Double missionConquest) {
		this.missionConquest = missionConquest;
	}

	public Double getMissionCounterattack() {
		return missionCounterattack;
	}

	public void setMissionCounterattack(Double missionCounterattack) {
		this.missionCounterattack = missionCounterattack;
	}

	@Override
	public ObjectEnum getObject() {
		return ObjectEnum.SPEED_IMPACT_GROUP;
	}

}
