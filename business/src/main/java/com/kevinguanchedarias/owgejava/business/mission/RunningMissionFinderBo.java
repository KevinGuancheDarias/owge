package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.planet.PlanetCleanerService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.ObtainedUnitUtil;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class RunningMissionFinderBo {
    private final MissionRepository missionRepository;
    private final PlanetRepository planetRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final HiddenUnitBo hiddenUnitBo;
    private final PlanetExplorationService planetExplorationService;
    private final PlanetCleanerService planetCleanerService;
    private final UserStorageRepository userStorageRepository;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;

    public List<UnitRunningMissionDto> findEnemyRunningMissions(UserStorage user) {
        List<Planet> myPlanets = planetRepository.findByOwnerId(user.getId());
        return missionRepository.findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(myPlanets, user)
                .stream().map(current -> {
                    current.setInvolvedUnits(obtainedUnitRepository.findByMissionId(current.getId()));
                    UnitRunningMissionDto retVal = new UnitRunningMissionDto(current);
                    retVal.nullifyInvolvedUnitsPlanets();
                    if (!planetExplorationService.isExplored(user, current.getSourcePlanet())) {
                        retVal.setSourcePlanet(null);
                        retVal.setUser(null);
                    }
                    hiddenUnitBo.defineHidden(current.getInvolvedUnits(), retVal.getInvolvedUnits());
                    ObtainedUnitUtil.handleInvisible(retVal.getInvolvedUnits());
                    return retVal;
                }).toList();
    }

    @TaggableCacheable(tags = Mission.MISSION_BY_USER_CACHE_TAG + ":#userId")
    public Integer countUserRunningMissions(Integer userId) {
        return missionRepository.countByUserIdAndResolvedFalse(userId);
    }

    /**
     * Returns all the running missions for the specified user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @TaggableCacheable(tags = {
            Mission.MISSION_BY_USER_CACHE_TAG + ":#userId",
            ObtainedUnit.OBTAINED_UNIT_CACHE_TAG_BY_USER + ":#userId"
    }, keySuffix = "#userId")
    public List<UnitRunningMissionDto> findUserRunningMissions(Integer userId) {
        var user = userStorageRepository.getReferenceById(userId);
        return missionRepository.findByUserIdAndResolvedFalse(userId).stream().
                map(UnitRunningMissionDto::new).map(current -> {
                    current.setInvolvedUnits(obtainedUnitFinderBo.findCompletedAsDto(
                            user,
                            obtainedUnitRepository.findByMissionId(current.getMissionId())
                    ));
                    if (current.getType() == MissionType.EXPLORE) {
                        planetCleanerService.cleanUpUnexplored(userId, current.getTargetPlanet());
                    }
                    return current;
                }).map(UnitRunningMissionDto::nullifyInvolvedUnitsPlanets).toList();
    }
}
