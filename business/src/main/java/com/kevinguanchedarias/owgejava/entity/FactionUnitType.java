package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Allows to apply faction overrides to specified unit type
 *
 * @since 0.10.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "factions_unit_types")
public class FactionUnitType {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "faction_id")
	private Faction faction;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "unit_type_id")
	private UnitType unitType;

	@Column(name = "max_couunt", nullable = true)
	private Long maxCount;

	/**
	 * @return the id
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the faction
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Faction getFaction() {
		return faction;
	}

	/**
	 * @param faction the faction to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setFaction(Faction faction) {
		this.faction = faction;
	}

	/**
	 * @return the unitType
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UnitType getUnitType() {
		return unitType;
	}

	/**
	 * @param unitType the unitType to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setUnitType(UnitType unitType) {
		this.unitType = unitType;
	}

	/**
	 * @return the maxCount
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getMaxCount() {
		return maxCount;
	}

	/**
	 * @param maxCount the maxCount to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setMaxCount(Long maxCount) {
		this.maxCount = maxCount;
	}

}
