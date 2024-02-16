package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class MissionFinderBo {
    private final MissionRepository missionRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final MissionTypeBo missionTypeBo;
    private final PlanetRepository planetRepository;
    private final ObjectRelationBo objectRelationBo;

    /**
     * finds user <b>not resolved</b> deployed mission, if none exists creates one
     * <br>
     * <b>IMPORTANT:</b> Will save the unit, because if the mission exists, has to
     * remove the firstDeploymentMission
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    @Transactional
    public Mission findDeployedMissionOrCreate(ObtainedUnit unit) {
        UserStorage user = unit.getUser();
        Planet origin = unit.getSourcePlanet();
        Planet target = unit.getTargetPlanet();
        var existingMissionOpt = missionRepository.findByUserIdAndTypeCodeAndTargetPlanetIdAndResolvedFalse(
                user.getId(), MissionType.DEPLOYED.name(), target.getId()
        ).stream().findFirst();
        if (existingMissionOpt.isPresent()) {
            var existingMission = existingMissionOpt.get();
            existingMission.getInvolvedUnits().add(unit);
            unit.setMission(existingMission);
            obtainedUnitRepository.save(unit);
            return existingMission;
        } else {
            Mission deployedMission = new Mission();
            deployedMission.setType(missionTypeBo.find(MissionType.DEPLOYED));
            deployedMission.setUser(user);
            deployedMission.setInvolvedUnits(new ArrayList<>());
            deployedMission.getInvolvedUnits().add(unit);
            deployedMission.setSourcePlanet(origin);
            deployedMission.setTargetPlanet(target);
            var savedDeployedMission = missionRepository.save(deployedMission);
            obtainedUnitRepository.save(unit);
            return savedDeployedMission;
        }
    }

    public RunningUnitBuildDto findRunningUnitBuild(Integer userId, Double planetId) {
        var mission = missionRepository.findByUserIdAndTypeCodeAndMissionInformationValue(userId, MissionType.BUILD_UNIT.name(), planetId);
        if (mission != null) {
            var missionInformation = mission.getMissionInformation();
            var unit = (Unit) objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
            return new RunningUnitBuildDto(unit, mission, SpringRepositoryUtil.findByIdOrDie(planetRepository, planetId.longValue()),
                    obtainedUnitRepository.findByMissionId(mission.getId()).get(0).getCount());
        } else {
            return null;
        }
    }

    /**
     * Finds all build missions for given user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<RunningUnitBuildDto> findBuildMissions(Integer userId) {
        return missionRepository.findByUserIdAndTypeCodeAndResolvedFalse(userId, MissionType.BUILD_UNIT.name()).stream()
                .map(mission -> {
                    var missionInformation = mission.getMissionInformation();
                    var unit = (Unit) objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
                    var planet = SpringRepositoryUtil.findByIdOrDie(planetRepository, missionInformation.getValue().longValue());
                    var findByMissionId = obtainedUnitRepository.findByMissionId(mission.getId());
                    return new RunningUnitBuildDto(unit, mission, planet,
                            findByMissionId.isEmpty() ? 0 : findByMissionId.get(0).getCount());
                }).toList();
    }
}
