package com.kevinguanchedarias.owgejava.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;

@MappedSuperclass
public abstract class EntityWithMissionLimitation<K extends Serializable> implements EntityWithId<K> {
	private static final long serialVersionUID = 3396208017507627247L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private K id;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_explore", nullable = false)
	private MissionSupportEnum canExplore = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_gather", nullable = false)
	private MissionSupportEnum canGather = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_establish_base", nullable = false)
	private MissionSupportEnum canEstablishBase = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_attack", nullable = false)
	private MissionSupportEnum canAttack = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_counterattack", nullable = false)
	private MissionSupportEnum canCounterattack = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_conquest", nullable = false)
	private MissionSupportEnum canConquest = MissionSupportEnum.ANY;

	@Enumerated(EnumType.STRING)
	@Column(name = "can_deploy", nullable = false)
	private MissionSupportEnum canDeploy = MissionSupportEnum.ANY;

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public K getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	@Override
	public void setId(K id) {
		this.id = id;
	}

	/**
	 * @return the canExplore
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionSupportEnum getCanExplore() {
		return canExplore;
	}

	/**
	 * @param canExplore the canExplore to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanExplore(MissionSupportEnum canExplore) {
		this.canExplore = canExplore;
	}

	/**
	 * @return the canGather
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionSupportEnum getCanGather() {
		return canGather;
	}

	/**
	 * @param canGather the canGather to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanGather(MissionSupportEnum canGather) {
		this.canGather = canGather;
	}

	/**
	 * @return the canEstablishBase
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionSupportEnum getCanEstablishBase() {
		return canEstablishBase;
	}

	/**
	 * @param canEstablishBase the canEstablishBase to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanEstablishBase(MissionSupportEnum canEstablishBase) {
		this.canEstablishBase = canEstablishBase;
	}

	/**
	 * @return the canAttack
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionSupportEnum getCanAttack() {
		return canAttack;
	}

	/**
	 * @param canAttack the canAttack to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanAttack(MissionSupportEnum canAttack) {
		this.canAttack = canAttack;
	}

	/**
	 * @return the canCounterattack
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionSupportEnum getCanCounterattack() {
		return canCounterattack;
	}

	/**
	 * @param canCounterattack the canCounterattack to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanCounterattack(MissionSupportEnum canCounterattack) {
		this.canCounterattack = canCounterattack;
	}

	/**
	 * @return the canConquest
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionSupportEnum getCanConquest() {
		return canConquest;
	}

	/**
	 * @param canConquest the canConquest to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanConquest(MissionSupportEnum canConquest) {
		this.canConquest = canConquest;
	}

	/**
	 * @return the canDeploy
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionSupportEnum getCanDeploy() {
		return canDeploy;
	}

	/**
	 * @param canDeploy the canDeploy to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setCanDeploy(MissionSupportEnum canDeploy) {
		this.canDeploy = canDeploy;
	}

}
