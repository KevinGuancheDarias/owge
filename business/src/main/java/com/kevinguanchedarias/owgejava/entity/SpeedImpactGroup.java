package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@Entity
@Table(name = "speed_impact_groups")
public class SpeedImpactGroup implements SimpleIdEntity {
	private static final long serialVersionUID = 8120163868349636675L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 50)
	private String name;

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

	@Override
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

}
