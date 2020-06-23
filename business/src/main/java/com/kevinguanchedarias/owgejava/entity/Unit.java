package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.owgejava.entity.listener.UnitListener;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

@Entity
@Table(name = "units")
@EntityListeners(UnitListener.class)
public class Unit extends CommonEntityWithImageStore<Integer>
		implements EntityWithImprovements<Integer>, WithDtoFromEntityTrait<Unit> {
	private static final long serialVersionUID = -1923291486680931835L;

	@Column(name = "order_number")
	private Integer order;

	private Integer points;
	private Integer time;

	@Column(name = "primary_resource")
	private Integer primaryResource;

	@Column(name = "secondary_resource")
	private Integer secondaryResource;

	private Integer energy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type")
	@Fetch(FetchMode.JOIN)
	private UnitType type;

	private Integer attack;

	@Column(name = "health", nullable = false)
	private Integer health = 1;
	private Integer shield;
	private Integer charge;

	@Column(name = "is_unique", nullable = false)
	private Boolean isUnique = false;

	@Column
	private Double speed = 0D;

	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "improvement_id")
	@Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE })
	private Improvement improvement;

	@Column(name = "cloned_improvements")
	private Boolean clonedImprovements = false;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "speed_impact_group_id")
	@Fetch(FetchMode.JOIN)
	private SpeedImpactGroup speedImpactGroup;

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
		this.time = time;
	}

	public Integer getPrimaryResource() {
		return primaryResource;
	}

	public void setPrimaryResource(Integer primaryResource) {
		this.primaryResource = primaryResource;
	}

	public Integer getSecondaryResource() {
		return secondaryResource;
	}

	public void setSecondaryResource(Integer secondaryResource) {
		this.secondaryResource = secondaryResource;
	}

	public Integer getEnergy() {
		return energy;
	}

	public void setEnergy(Integer energy) {
		this.energy = energy;
	}

	public UnitType getType() {
		return type;
	}

	public void setType(UnitType type) {
		this.type = type;
	}

	public Integer getAttack() {
		return attack;
	}

	public void setAttack(Integer attack) {
		this.attack = attack;
	}

	public Integer getHealth() {
		return health;
	}

	public void setHealth(Integer health) {
		this.health = health;
	}

	public Integer getShield() {
		return shield;
	}

	public void setShield(Integer shield) {
		this.shield = shield;
	}

	public Integer getCharge() {
		return charge;
	}

	public void setCharge(Integer charge) {
		this.charge = charge;
	}

	public Boolean getIsUnique() {
		return isUnique;
	}

	public void setIsUnique(Boolean isUnique) {
		this.isUnique = isUnique;
	}

	/**
	 * @return the speed
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double getSpeed() {
		return speed;
	}

	/**
	 * @param speed the speed to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	@Override
	public Improvement getImprovement() {
		return improvement;
	}

	@Override
	public void setImprovement(Improvement improvement) {
		this.improvement = improvement;
	}

	@Override
	public Boolean getClonedImprovements() {
		return clonedImprovements;
	}

	@Override
	public void setClonedImprovements(Boolean clonedImprovements) {
		this.clonedImprovements = clonedImprovements;
	}

	/**
	 * @return the speedImpactGroup
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SpeedImpactGroup getSpeedImpactGroup() {
		return speedImpactGroup;
	}

	/**
	 * @param speedImpactGroup the speedImpactGroup to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSpeedImpactGroup(SpeedImpactGroup speedImpactGroup) {
		this.speedImpactGroup = speedImpactGroup;
	}

}
