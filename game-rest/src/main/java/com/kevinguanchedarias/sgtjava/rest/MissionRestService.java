package com.kevinguanchedarias.sgtjava.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.sgtjava.business.MissionBo;
import com.kevinguanchedarias.sgtjava.business.UnitMissionBo;
import com.kevinguanchedarias.sgtjava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.sgtjava.pojo.UnitMissionInformation;

@RestController
@RequestMapping("mission")
@ApplicationScope
public class MissionRestService {

	private static final String TARGET_CONSUMES_MEDIATYPE = "application/json";

	@Autowired
	private UnitMissionBo unitMissionBo;

	@Autowired
	private MissionBo missionBo;

	@RequestMapping(value = "explorePlanet", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public UnitRunningMissionDto explorePlanet(@RequestBody UnitMissionInformation missionInformation) {
		return unitMissionBo.myRegisterExploreMission(missionInformation);
	}

	@RequestMapping(value = "gather", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public UnitRunningMissionDto gather(@RequestBody UnitMissionInformation missionInformation) {
		return unitMissionBo.myRegisterGatherMission(missionInformation);
	}

	@RequestMapping(value = "establishBase", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public UnitRunningMissionDto establishBase(@RequestBody UnitMissionInformation missionInformation) {
		return unitMissionBo.myRegisterEstablishBaseMission(missionInformation);
	}

	@RequestMapping(value = "attack", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public UnitRunningMissionDto attack(@RequestBody UnitMissionInformation missionInformation) {
		return unitMissionBo.myRegisterAttackMission(missionInformation);
	}

	@RequestMapping(value = "counterattack", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public UnitRunningMissionDto counterattack(@RequestBody UnitMissionInformation missionInformation) {
		return unitMissionBo.myRegisterCounterattackMission(missionInformation);
	}

	@RequestMapping(value = "conquest", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public UnitRunningMissionDto conquest(@RequestBody UnitMissionInformation missionInformation) {
		return unitMissionBo.myRegisterConquestMission(missionInformation);
	}

	@RequestMapping(value = "cancel", method = RequestMethod.POST, consumes = TARGET_CONSUMES_MEDIATYPE)
	public String cancel(@RequestParam("id") Long id) {
		unitMissionBo.myCancelMission(id);
		return "OK";
	}

	@RequestMapping(value = "findMy", method = RequestMethod.GET)
	public List<UnitRunningMissionDto> findMy() {
		return missionBo.myFindUserRunningMissions();
	}

	@RequestMapping(value = "findEnemy", method = RequestMethod.GET)
	public List<UnitRunningMissionDto> findEnemy() {
		return missionBo.myFindEnemyRunningMissions();
	}
}
