package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.pojo.DeprecationRestResponse;
import com.kevinguanchedarias.owgejava.pojo.UnitWithRequirementInformation;

@RestController
@RequestMapping("game/unit")
@ApplicationScope
public class UnitRestService implements SyncSource {

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
	private FactionBo factionBo;

	/**
	 *
	 * @deprecated Find in all planets instead
	 * @param planetId
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.9.0")
	@GetMapping("findRunning")
	public Object findRunning(@RequestParam("planetId") Double planetId) {
		RunningUnitBuildDto retVal = missionBo.findRunningUnitBuild(findLoggedInUser().getId(), planetId);
		if (retVal == null) {
			return "";
		}

		return new DeprecationRestResponse<>("0.9.0", "/unit/build-missions", retVal);
	}

	@PostMapping(value = "build")
	public Object build(@RequestParam("planetId") Long planetId, @RequestParam("unitId") Integer unitId,
			@RequestParam("count") Long count) {
		Integer userId = findLoggedInUser().getId();
		RunningUnitBuildDto retVal = missionBo.registerBuildUnit(userId, planetId, unitId, count);
		if (retVal == null) {
			return "";
		}
		retVal.setMissionsCount(missionBo.countUserMissions(userId));
		return retVal;
	}

	@GetMapping("cancel")
	public String cancel(@RequestParam("missionId") Long missionId) {
		missionBo.cancelBuildUnit(missionId);
		return "\"OK\"";
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	public String delete(@RequestBody ObtainedUnitDto obtainedUnitDto) {
		obtainedUnitDto.setUserId(userStorageBo.findLoggedIn().getId());
		obtainedUnitBo.saveWithSubtraction(obtainedUnitDto, true);
		return "\"OK\"";
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("unit_unlocked_change", this::findUnlocked)
				.withHandler("unit_build_mission_change", user -> missionBo.findBuildMissions(user.getId()))
				.withHandler("unit_obtained_change", this::findInMyPlanets)
				.withHandler("unit_requirements_change", this::requirements).build();
	}

	private List<UnitDto> findUnlocked(UserStorage user) {
		List<Unit> units = unlockedRelationBo.unboxToTargetEntity(
				unlockedRelationBo.findByUserIdAndObjectType(user.getId(), RequirementTargetObject.UNIT));

		units.forEach(current -> Hibernate.initialize(current.getInterceptableSpeedGroups()));
		var convert = new UnitDto();
		return convert.dtoFromEntity(UnitDto.class, units);
	}

	private List<ObtainedUnitDto> findInMyPlanets(UserStorage user) {
		List<ObtainedUnit> entities = obtainedUnitBo.findDeployedInUserOwnedPlanets(user.getId());
		entities.forEach(current -> current.getUnit().getSpeedImpactGroup().setRequirementGroups(null));
		entities.forEach(current -> Hibernate.initialize(current.getUnit().getInterceptableSpeedGroups()));
		return obtainedUnitBo.toDto(entities);
	}

	private UserStorage findLoggedInUser() {
		return userStorageBo.findLoggedIn();
	}

	private List<UnitWithRequirementInformation> requirements(UserStorage user) {
		return requirementBo.findFactionUnitLevelRequirements(factionBo.findByUser(user.getId())).stream()
				.filter(unitWithRequirementInformation -> unitWithRequirementInformation.getUnit()
						.getHasToDisplayInRequirements())
				.map(current -> {
					final UnitDto unit = current.getUnit();
					unit.setImprovement(null);
					unit.setSpeedImpactGroup(null);
					current.getRequirements().forEach(requirement -> requirement.getUpgrade().setRequirements(null));
					return current;
				}).collect(Collectors.toList());
	}
}
