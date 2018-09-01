package com.kevinguanchedarias.sgtjava.pojo;

import com.kevinguanchedarias.sgtjava.business.UserStorageBo;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;

public class ResourceRequirementsPojo {
	private Double requiredPrimary;
	private Double requiredSecondary;
	private Double requiredEnergy;
	private Double requiredTime;

	/**
	 * Checks if the user mets the requirement
	 * 
	 * @param user
	 *            <b>MUST BE</b> a fully loaded user
	 * @param userStorageBo
	 *            Used to get the user energy
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public boolean canRun(UserStorage user, UserStorageBo userStorageBo) {
		return user.getPrimaryResource() >= requiredPrimary && user.getSecondaryResource() >= requiredSecondary
				&& (requiredEnergy == null || userStorageBo.findAvailableEnergy(user) >= requiredEnergy);
	}

	public Double getRequiredPrimary() {
		return requiredPrimary;
	}

	public void setRequiredPrimary(Double requiredPrimary) {
		this.requiredPrimary = requiredPrimary;
	}

	public Double getRequiredSecondary() {
		return requiredSecondary;
	}

	public void setRequiredSecondary(Double requiredSecondary) {
		this.requiredSecondary = requiredSecondary;
	}

	public Double getRequiredEnergy() {
		return requiredEnergy;
	}

	public void setRequiredEnergy(Double requiredEnergy) {
		this.requiredEnergy = requiredEnergy;
	}

	public Double getRequiredTime() {
		return requiredTime;
	}

	public void setRequiredTime(Double requiredTime) {
		this.requiredTime = requiredTime;
	}

}
