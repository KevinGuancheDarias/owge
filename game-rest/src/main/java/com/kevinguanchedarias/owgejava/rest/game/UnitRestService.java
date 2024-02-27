package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.*;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.pojo.DeprecationRestResponse;
import com.kevinguanchedarias.owgejava.pojo.UnitWithRequirementInformation;
import com.kevinguanchedarias.owgejava.responses.CriticalAttackInformationResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("game/unit")
@ApplicationScope
@AllArgsConstructor
public class UnitRestService implements SyncSource {

    private final UserSessionService userSessionService;
    private final MissionBo missionBo;
    private final MissionFinderBo missionFinderBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final RequirementBo requirementBo;
    private final FactionBo factionBo;
    private final UnitBo unitBo;
    private final CriticalAttackBo criticalAttackBo;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Find in all planets instead
     */
    @Deprecated(since = "0.9.0")
    @GetMapping("findRunning")
    public Object findRunning(@RequestParam("planetId") Double planetId) {
        RunningUnitBuildDto retVal = missionFinderBo.findRunningUnitBuild(findLoggedInUser().getId(), planetId);
        if (retVal == null) {
            return "";
        }

        return new DeprecationRestResponse<>("0.9.0", "/unit/build-missions", retVal);
    }

    @PostMapping(value = "build")
    public void build(@RequestParam("planetId") Long planetId, @RequestParam("unitId") Integer unitId,
                      @RequestParam("count") Long count) {
        Integer userId = findLoggedInUser().getId();
        missionBo.registerBuildUnit(userId, planetId, unitId, count);
    }

    @GetMapping("cancel")
    public String cancel(@RequestParam("missionId") Long missionId) {
        missionBo.cancelBuildUnit(missionId);
        return "\"OK\"";
    }

    @PostMapping("delete")
    public String delete(@RequestBody ObtainedUnitDto obtainedUnitDto) {
        obtainedUnitDto.setUserId(userSessionService.findLoggedIn().getId());
        obtainedUnitBo.saveWithSubtraction(obtainedUnitDto, true);
        return "\"OK\"";
    }

    @GetMapping("{unitId}/criticalAttack")
    public List<CriticalAttackInformationResponse> findCriticalAttackInformation(@PathVariable int unitId) {
        var criticalAttack = unitBo.findUsedCriticalAttack(unitId);
        return criticalAttack == null
                ? List.of()
                : criticalAttackBo.buildFullInformation(criticalAttack);
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("unit_unlocked_change", this::findUnlocked)
                .withHandler("unit_build_mission_change", user -> missionFinderBo.findBuildMissions(user.getId()))
                .withHandler("unit_obtained_change", this::findInMyPlanets)
                .withHandler("unit_requirements_change", this::requirements).build();
    }

    private List<UnitDto> findUnlocked(UserStorage user) {
        return unitBo.findAllByUser(user);
    }

    private List<ObtainedUnitDto> findInMyPlanets(UserStorage user) {
        return obtainedUnitFinderBo.findCompletedAsDto(user);
    }

    private UserStorage findLoggedInUser() {
        return userSessionService.findLoggedIn();
    }

    private List<UnitWithRequirementInformation> requirements(UserStorage user) {
        return requirementBo.findFactionUnitLevelRequirements(factionBo.findByUser(user.getId())).stream()
                .filter(unitWithRequirementInformation -> unitWithRequirementInformation.getUnit()
                        .getHasToDisplayInRequirements())
                .map(current -> {
                    UnitDto unit = current.getUnit();
                    unit.setImprovement(null);
                    unit.setSpeedImpactGroup(null);
                    current.getRequirements().forEach(requirement -> requirement.getUpgrade().setRequirements(null));
                    return current;
                }).toList();
    }
}
