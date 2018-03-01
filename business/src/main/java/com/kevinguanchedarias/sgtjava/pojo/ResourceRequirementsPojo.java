package com.kevinguanchedarias.sgtjava.pojo;

import com.kevinguanchedarias.sgtjava.entity.UserStorage;

public class ResourceRequirementsPojo {
	private Double requiredPrimary;
	private Double requiredSecondary;
	private Double requiredTime;

	/**
	 * Checks if the user mets the requirement
	 * 
	 * @param user
	 *            <b>MUST BE</b> a fully loaded user
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public boolean canRun(UserStorage user) {
		return user.getPrimaryResource() >= requiredPrimary && user.getSecondaryResource() >= requiredSecondary;
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

	public Double getRequiredTime() {
		return requiredTime;
	}

	public void setRequiredTime(Double requiredTime) {
		this.requiredTime = requiredTime;
	}

}
