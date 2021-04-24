package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

import javax.persistence.*;

import com.kevinguanchedarias.owgejava.entity.listener.EntityWithRelationListener;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithRequirementGroupsListener;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a speed group, the special conditions to move units <br>
 * Notice: The requirements mean the unit having this speed group will be able
 * to cross galaxies
 *
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "speed_impact_groups")
@EntityListeners({ EntityWithRelationListener.class, EntityWithRequirementGroupsListener.class })
public class SpeedImpactGroup extends EntityWithMissionLimitation<Integer> implements EntityWithRequirementGroups {
	private static final long serialVersionUID = 8120163868349636675L;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id")
	@Getter
	@Setter
	private ImageStore image;

	@Transient
	private ObjectRelation relation;

	@Transient
	private transient List<RequirementGroup> requirementGroups;

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

	/**
	 * @return the relation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public ObjectRelation getRelation() {
		return relation;
	}

	/**
	 * @param relation the relation to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	@Override
	public void setRelation(ObjectRelation relation) {
		this.relation = relation;
	}

	/**
	 * @return the requirementGroups
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public List<RequirementGroup> getRequirementGroups() {
		return requirementGroups;
	}

	/**
	 * @param requirementGroups the requirementGroups to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	@Override
	public void setRequirementGroups(List<RequirementGroup> requirementGroups) {
		this.requirementGroups = requirementGroups;
	}

	@Override
	public ObjectEnum getObject() {
		return ObjectEnum.SPEED_IMPACT_GROUP;
	}
}
