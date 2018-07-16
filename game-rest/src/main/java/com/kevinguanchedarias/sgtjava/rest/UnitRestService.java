package com.kevinguanchedarias.sgtjava.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.sgtjava.business.MissionBo;
import com.kevinguanchedarias.sgtjava.business.ObtainedUnitBo;
import com.kevinguanchedarias.sgtjava.business.RequirementBo;
import com.kevinguanchedarias.sgtjava.business.UnlockedRelationBo;
import com.kevinguanchedarias.sgtjava.business.UserStorageBo;
import com.kevinguanchedarias.sgtjava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.sgtjava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.sgtjava.dto.UnitDto;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.pojo.UnitWithRequirementInformation;
import com.kevinguanchedarias.sgtjava.util.DtoUtilService;

@RestController
@RequestMapping("unit")
@ApplicationScope
public class UnitRestService {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private UnlockedRelationBo unlockedRelationBo;

	@Autowired
	private MissionBo missionBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private RequirementBo requirementBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	@RequestMapping(value = "findUnlocked", method = RequestMethod.GET)
	public Object findUnlocked() {
		List<Unit> units = unlockedRelationBo.unboxToTargetEntity(
				unlockedRelationBo.findByUserIdAndObjectType(findLoggedInUser().getId(), RequirementTargetObject.UNIT));

		UnitDto convert = new UnitDto();
		return convert.dtoFromEntity(UnitDto.class, units);
	}

	@RequestMapping(value = "findRunning", method = RequestMethod.GET)
	public Object findRunning(@RequestParam("planetId") Double planetId) {
		RunningUnitBuildDto retVal = missionBo.findRunningUnitBuild(findLoggedInUser().getId(), planetId);
		if (retVal == null) {
			return "";
		}

		return retVal;
	}

	@RequestMapping(value = "build", method = RequestMethod.GET)
	public Object build(@RequestParam("planetId") Long planetId, @RequestParam("unitId") Integer unitId,
			@RequestParam("count") Long count) {
		RunningUnitBuildDto retVal = missionBo.registerBuildUnit(findLoggedInUser().getId(), planetId, unitId, count);
		if (retVal == null) {
			return "";
		}

		return retVal;
	}

	@RequestMapping(value = "requirements", method = RequestMethod.GET)
	public List<UnitWithRequirementInformation> requirements() {
		return requirementBo.computeReachedLevel(findLoggedInUser(), requirementBo
				.findFactionUnitLevelRequirements(userStorageBo.findLoggedInWithDetails(false).getFaction()));
	}

	@RequestMapping(value = "cancel", method = RequestMethod.GET)
	public Object cancel(@RequestParam("missionId") Long missionId) {
		missionBo.cancelBuildUnit(missionId);
		return "OK";
	}

	@RequestMapping(value = "findInMyPlanet", method = RequestMethod.GET)
	public List<ObtainedUnitDto> findInMyPlanet(@RequestParam("planetId") Long planetId) {
		return dtoUtilService.convertEntireArray(ObtainedUnitDto.class, obtainedUnitBo.findInMyPlanet(planetId));
	}

	private UserStorage findLoggedInUser() {
		return userStorageBo.findLoggedIn();
	}
}
