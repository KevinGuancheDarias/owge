package com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns;

import com.kevinguanchedarias.owgejava.business.MissionSchedulerService;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTimeManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTypeBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@AllArgsConstructor
public class ReturnMissionRegistrationBo {
    private final MissionTypeBo missionTypeBo;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final MissionRepository missionRepository;
    private final MissionSchedulerService missionSchedulerService;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final PlanetLockUtilService planetLockUtilService;

    @Transactional
    public void registerReturnMission(Mission mission, Double customRequiredTime) {
        planetLockUtilService.doInsideLock(
                List.of(mission.getSourcePlanet(), mission.getTargetPlanet()),
                () -> doRegisterReturnMission(mission, customRequiredTime)
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void doRegisterReturnMission(Mission originMission, Double customRequiredTime) {
        Mission returnMission = new Mission();
        returnMission.setStartingDate(LocalDateTime.now(ZoneOffset.UTC));
        returnMission.setType(missionTypeBo.find(MissionType.RETURN_MISSION));
        Double requiredTime = customRequiredTime == null ? originMission.getRequiredTime() : customRequiredTime;
        returnMission.setRequiredTime(requiredTime);
        returnMission.setTerminationDate(missionTimeManagerBo.computeTerminationDate(requiredTime));
        returnMission.setSourcePlanet(originMission.getSourcePlanet());
        returnMission.setTargetPlanet(originMission.getTargetPlanet());
        returnMission.setUser(originMission.getUser());
        returnMission.setRelatedMission(originMission);
        returnMission.setInvisible(Boolean.TRUE.equals(originMission.getInvisible()));
        List<ObtainedUnit> obtainedUnits = obtainedUnitRepository.findByMissionId(originMission.getId());
        missionRepository.saveAndFlush(returnMission);
        obtainedUnits.forEach(current -> current.setMission(returnMission));
        obtainedUnitRepository.saveAll(obtainedUnits);
        missionSchedulerService.scheduleMission(UnitMissionBo.JOB_GROUP_NAME, returnMission);
        missionEventEmitterBo.emitLocalMissionChangeAfterCommit(returnMission);
    }
}
