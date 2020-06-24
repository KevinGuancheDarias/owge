package com.kevinguanchedarias.owgejava.dto.base;

import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class DtoWithMissionLimitation {
	private MissionSupportEnum canExplore = MissionSupportEnum.ANY;
	private MissionSupportEnum canGather = MissionSupportEnum.ANY;
	private MissionSupportEnum canEstablishBase = MissionSupportEnum.ANY;
	private MissionSupportEnum canAttack = MissionSupportEnum.ANY;
	private MissionSupportEnum canCounterattack = MissionSupportEnum.ANY;
	private MissionSupportEnum canConquest = MissionSupportEnum.ANY;
	private MissionSupportEnum canDeploy = MissionSupportEnum.ANY;

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
