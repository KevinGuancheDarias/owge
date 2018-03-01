package com.kevinguanchedarias.sgtjava.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.sgtjava.business.MissionBo;
import com.kevinguanchedarias.sgtjava.business.ObtainedUpradeBo;
import com.kevinguanchedarias.sgtjava.business.UserStorageBo;
import com.kevinguanchedarias.sgtjava.dto.ObtainedUpgradeDto;
import com.kevinguanchedarias.sgtjava.dto.RunningUpgradeDto;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUpgrade;

@RestController
@RequestMapping("upgrade")
@ApplicationScope
public class UpgradeRestService {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObtainedUpradeBo obtainedUpgradeBo;

	@Autowired
	private MissionBo missionBo;

	@RequestMapping(value = "findObtained", method = RequestMethod.GET)
	public Object findObtained() {
		List<ObtainedUpgrade> obtainedUpgradeList = obtainedUpgradeBo.findByUser(userStorageBo.findLoggedIn().getId());
		List<ObtainedUpgradeDto> obtainedUpgradeDtoList = new ArrayList<>();

		for (ObtainedUpgrade current : obtainedUpgradeList) {
			ObtainedUpgradeDto currentDto = new ObtainedUpgradeDto();
			currentDto.dtoFromEntity(current);
			obtainedUpgradeDtoList.add(currentDto);
		}

		return obtainedUpgradeDtoList;
	}

	@RequestMapping(value = "findRunningUpgrade", method = RequestMethod.GET)
	public Object findRunningUpgrade() {
		RunningUpgradeDto retVal = missionBo.findRunningLevelUpMission(userStorageBo.findLoggedIn().getId());

		if (retVal == null) {
			return "";
		}

		return retVal;
	}

	@RequestMapping(value = "registerLevelUp", method = RequestMethod.GET)
	public Object registerLevelUp(@RequestParam("upgradeId") Integer upgradeId) {
		Integer userId = userStorageBo.findLoggedIn().getId();
		missionBo.registerLevelUpAnUpgrade(userId, upgradeId);
		return missionBo.findRunningLevelUpMission(userId);
	}

	@RequestMapping(value = "cancelUpgrade", method = RequestMethod.GET)
	public Object cancelUpgrade() {
		missionBo.cancelUpgradeMission(userStorageBo.findLoggedIn().getId());
		return "{}";
	}
}
