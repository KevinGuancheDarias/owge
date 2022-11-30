package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ReturnMissionProcessor implements MissionProcessor {
    private final PlanetLockUtilService planetLockUtilService;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObtainedUnitBo obtainedUnitBo;
    private final AsyncRunnerBo asyncRunnerBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.RETURN_MISSION;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        planetLockUtilService.doInsideLock(List.of(mission.getSourcePlanet(), mission.getTargetPlanet()), () -> {
            var userId = mission.getUser().getId();
            var obtainedUnits = obtainedUnitRepository.findByMissionId(mission.getId());
            obtainedUnits.forEach(current -> obtainedUnitBo.moveUnit(current, userId, mission.getSourcePlanet().getId()));
            mission.setResolved(true);
            missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission);
            asyncRunnerBo
                    .runAssyncWithoutContextDelayed(
                            () -> obtainedUnitEventEmitter.emitObtainedUnits(mission.getUser()),
                            500);
        });
        return null;
    }
}
