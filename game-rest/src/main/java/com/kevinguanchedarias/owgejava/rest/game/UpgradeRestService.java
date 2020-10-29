package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

	@GetMapping("findObtained")
	public List<ObtainedUpgradeDto> findObtained() {
		List<ObtainedUpgrade> obtainedUpgradeList = obtainedUpgradeBo.findByUser(userStorageBo.findLoggedIn().getId());
		return obtainedUpgradeBo.toDto(obtainedUpgradeList);
	}

	/**
	 * Finds one single obtained unit by user and upgrade id
	 *
	 * @param id
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("findObtained/{id}")
	public ObtainedUpgradeDto findObtanedById(@PathVariable Integer id) {
		return obtainedUpgradeBo
				.toDto(obtainedUpgradeBo.findByUserAndUpgrade(userStorageBo.findLoggedIn().getId(), id));
	}

	@GetMapping("findRunningUpgrade")
	public RunningUpgradeDto findRunningUpgrade() {
		return missionBo.findRunningLevelUpMission(userStorageBo.findLoggedIn().getId());
	}

	@GetMapping("registerLevelUp")
	public RunningUpgradeDto registerLevelUp(@RequestParam("upgradeId") Integer upgradeId) {
		Integer userId = userStorageBo.findLoggedIn().getId();
		missionBo.registerLevelUpAnUpgrade(userId, upgradeId);
		RunningUpgradeDto retVal = missionBo.findRunningLevelUpMission(userId);
		retVal.setMissionsCount(missionBo.countUserMissions(userId));
		return retVal;
	}

	@RequestMapping(value = "cancelUpgrade", method = RequestMethod.GET)
	public Object cancelUpgrade() {
		missionBo.cancelUpgradeMission(userStorageBo.findLoggedIn().getId());
		return "{}";
	}

	@Override
	public Map<String, Supplier<Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("obtained_upgrades_change", this::findObtained)
				.withHandler("running_upgrade_change", this::findRunningUpgrade).build();
	}
}
