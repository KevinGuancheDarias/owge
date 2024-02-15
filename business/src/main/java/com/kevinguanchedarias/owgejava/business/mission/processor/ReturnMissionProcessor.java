package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class ReturnMissionProcessor implements MissionProcessor {
    private final PlanetLockUtilService planetLockUtilService;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObtainedUnitBo obtainedUnitBo;
    private final AsyncRunnerBo asyncRunnerBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final RequirementBo requirementBo;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.RETURN_MISSION;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        planetLockUtilService.doInsideLock(List.of(mission.getSourcePlanet(), mission.getTargetPlanet()), () -> {
            log.debug("Processing return mission {}", mission.getId());
            var user = mission.getUser();
            var userId = user.getId();
            var planetOwnerOpt = Optional.ofNullable(mission.getSourcePlanet().getOwner());
            var obtainedUnits = obtainedUnitRepository.findByMissionId(mission.getId());
            obtainedUnits.forEach(current -> obtainedUnitBo.moveUnit(current, userId, mission.getSourcePlanet().getId()));
            mission.setResolved(true);
            missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission);
            asyncRunnerBo
                    .runAsyncWithoutContextDelayed(
                            () -> {
                                if (planetOwnerOpt.isPresent() && planetOwnerOpt.get().getId().equals(userId)) {
                                    obtainedUnits.stream().map(ObtainedUnit::getUnit).forEach(current -> {
                                        requirementBo.triggerUnitBuildCompletedOrKilled(user, current);
                                        requirementBo.triggerUnitAmountChanged(user, current);
                                    });
                                }
                                obtainedUnitEventEmitter.emitObtainedUnits(mission.getUser());
                            },
                            500);
            log.debug("Done processing return mission {}", mission.getId());
        });
        return null;
    }
}
