package com.kevinguanchedarias.sgtjava.pojo;

import com.kevinguanchedarias.sgtjava.dto.UpgradeDto;

public class UnitUpgradeRequirements {
	private UpgradeDto upgrade;
	private Integer level;
	private boolean reached = false;

	public UpgradeDto getUpgrade() {
		return upgrade;
	}

	public void setUpgrade(UpgradeDto upgrade) {
		this.upgrade = upgrade;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	/**
	 * If true, means the user got the ugrade to the required level
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean isReached() {
		return reached;
	}

	public void setReached(boolean reached) {
		this.reached = reached;
	}

}
