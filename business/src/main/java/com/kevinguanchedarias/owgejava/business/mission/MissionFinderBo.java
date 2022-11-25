package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@AllArgsConstructor
public class MissionFinderBo {
    private final MissionRepository missionRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final MissionTypeBo missionTypeBo;

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
            unit.setFirstDeploymentMission(null);
            unit.setMission(existingMission);
            obtainedUnitRepository.save(unit);
            return existingMission;
        } else {
            final Mission deployedMission = new Mission();
            deployedMission.setType(missionTypeBo.find(MissionType.DEPLOYED));
            deployedMission.setUser(user);
            deployedMission.setInvolvedUnits(new ArrayList<>());
            deployedMission.getInvolvedUnits().add(unit);
            if (unit.getFirstDeploymentMission() == null) {
                deployedMission.setSourcePlanet(origin);
                deployedMission.setTargetPlanet(target);
                var savedDeployedMission = missionRepository.save(deployedMission);
                unit.setFirstDeploymentMission(deployedMission);
                obtainedUnitRepository.save(unit);
                return savedDeployedMission;
            } else {
                missionRepository.findById(unit.getFirstDeploymentMission().getId())
                        .ifPresent(firstDeploymentMission -> {
                            deployedMission.setSourcePlanet(firstDeploymentMission.getSourcePlanet());
                            deployedMission.setTargetPlanet(firstDeploymentMission.getTargetPlanet());
                        });
                return missionRepository.save(deployedMission);
            }
        }
    }
}
