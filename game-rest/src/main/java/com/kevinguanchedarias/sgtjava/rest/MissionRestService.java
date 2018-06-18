package com.kevinguanchedarias.sgtjava.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

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
}
