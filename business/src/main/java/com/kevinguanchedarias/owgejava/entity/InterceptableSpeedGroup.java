package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents the capability that a {@link Unit} may have to intercept other
 * units having the desired {@link SpeedImpactGroup}
 *
 * @since 0.10.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "interceptable_speed_group")
public class InterceptableSpeedGroup implements EntityWithId<Integer> {
	private static final long serialVersionUID = 2487571740734931586L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "unit_id")
	private Unit unit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "speed_impact_group_id")
	private SpeedImpactGroup speedImpactGroup;

	/**
	 * @return the id
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the unit
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * @param unit the unit to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	/**
	 * @return the speedImpactGroup
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SpeedImpactGroup getSpeedImpactGroup() {
		return speedImpactGroup;
	}

	/**
	 * @param speedImpactGroup the speedImpactGroup to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setSpeedImpactGroup(SpeedImpactGroup speedImpactGroup) {
		this.speedImpactGroup = speedImpactGroup;
	}

}
