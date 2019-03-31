package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Upgrade;

public class RunningUpgradeDto extends AbstractRunningMissionDto {
	private UpgradeDto upgrade;
	private Integer level;

	public RunningUpgradeDto(Upgrade upgrade, Mission mission) {
		super(mission);
		UpgradeDto upgradeDto = new UpgradeDto();
		upgradeDto.dtoFromEntity(upgrade);

		this.upgrade = upgradeDto;
		level = mission.getMissionInformation().getValue().intValue();
	}

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

}
