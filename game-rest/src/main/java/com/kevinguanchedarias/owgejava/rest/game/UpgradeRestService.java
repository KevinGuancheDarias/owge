package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUpgradeBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUpgradeDto;
import com.kevinguanchedarias.owgejava.dto.RunningUpgradeDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

@RestController
@RequestMapping("game/upgrade")
@ApplicationScope
public class UpgradeRestService implements SyncSource {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObtainedUpgradeBo obtainedUpgradeBo;

	@Autowired
	private MissionBo missionBo;

	@GetMapping("registerLevelUp")
	public RunningUpgradeDto registerLevelUp(@RequestParam("upgradeId") Integer upgradeId) {
		Integer userId = userStorageBo.findLoggedIn().getId();
		missionBo.registerLevelUpAnUpgrade(userId, upgradeId);
		RunningUpgradeDto retVal = missionBo.findRunningLevelUpMission(userId);
		retVal.setMissionsCount(missionBo.countUserMissions(userId));
		return retVal;
	}

	@GetMapping("cancelUpgrade")
	public Object cancelUpgrade() {
		missionBo.cancelUpgradeMission(userStorageBo.findLoggedIn().getId());
		return "{}";
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("obtained_upgrades_change", this::findObtained)
				.withHandler("running_upgrade_change", this::findRunningUpgrade).build();
	}

	private List<ObtainedUpgradeDto> findObtained(UserStorage user) {
		List<ObtainedUpgrade> obtainedUpgradeList = obtainedUpgradeBo.findByUser(user.getId());
		return obtainedUpgradeBo.toDto(obtainedUpgradeList);
	}

	private RunningUpgradeDto findRunningUpgrade(UserStorage user) {
		return missionBo.findRunningLevelUpMission(user.getId());
	}
}
