package com.kevinguanchedarias.owgejava.rest.game;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;

@RestController
@RequestMapping("game/mission")
@ApplicationScope
public class MissionRestService implements SyncSource {

	private static final String TARGET_CONSUMES_MEDIATYPE = "application/json";

	@Autowired
	private UnitMissionBo unitMissionBo;

	@Autowired
	private MissionBo missionBo;

	@PostMapping(value = "explorePlanet", consumes = TARGET_CONSUMES_MEDIATYPE)
	public void explorePlanet(@RequestBody UnitMissionInformation missionInformation) {
		unitMissionBo.myRegisterExploreMission(missionInformation);
	}

	@RequestMapping(value = "gather", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public void gather(@RequestBody UnitMissionInformation missionInformation) {
		unitMissionBo.myRegisterGatherMission(missionInformation);
	}

	@RequestMapping(value = "establishBase", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public void establishBase(@RequestBody UnitMissionInformation missionInformation) {
		unitMissionBo.myRegisterEstablishBaseMission(missionInformation);
	}

	@RequestMapping(value = "attack", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public void attack(@RequestBody UnitMissionInformation missionInformation) {
		unitMissionBo.myRegisterAttackMission(missionInformation);
	}

	@RequestMapping(value = "counterattack", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public void counterattack(@RequestBody UnitMissionInformation missionInformation) {
		unitMissionBo.myRegisterCounterattackMission(missionInformation);
	}

	@RequestMapping(value = "conquest", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public void conquest(@RequestBody UnitMissionInformation missionInformation) {
		unitMissionBo.myRegisterConquestMission(missionInformation);
	}

	@RequestMapping(value = "deploy", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public void deploy(@RequestBody UnitMissionInformation missionInformation) {
		unitMissionBo.myRegisterDeploy(missionInformation);
	}

	@PostMapping("cancel")
	public String cancel(@RequestParam("id") Long id) {
		unitMissionBo.myCancelMission(id);
		return "\"OK\"";
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("missions_count_change", this::findCount)
				.withHandler("unit_mission_change",
						user -> new MissionWebsocketMessage(findCount(user),
								missionBo.findUserRunningMissions(user.getId())))
				.withHandler("enemy_mission_change", user -> missionBo.findEnemyRunningMissions(user)).build();
	}

	private Integer findCount(UserStorage user) {
		return missionBo.countUserMissions(user.getId());
	}
}
