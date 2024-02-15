package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionUnitsFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.util.EntityRefreshUtilService;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class DeployMissionProcessor implements MissionProcessor {
    private final MissionUnitsFinderBo missionUnitsFinderBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final TransactionUtilService transactionUtilService;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final EntityRefreshUtilService entityRefreshUtilService;
    private final HiddenUnitBo hiddenUnitBo;
    private final RequirementBo requirementBo;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.DEPLOY;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        long missionId = mission.getId();
        UserStorage user = mission.getUser();
        Integer userId = user.getId();
        var alteredUnits = missionUnitsFinderBo.findUnitsInvolved(missionId).stream()
                .map(current -> obtainedUnitBo.moveUnit(current, userId, mission.getTargetPlanet().getId()))
                .toList();

        var deployedMission = alteredUnits.getFirst().getMission();
        if (deployedMission != null) {
            deployedMission.setInvisible(deployedMission.getInvolvedUnits().stream().allMatch(
                    involvedUnit -> hiddenUnitBo.isHiddenUnit(user, involvedUnit.getUnit())
            ));
        }

        mission.setResolved(true);
        transactionUtilService.doAfterCommit(() -> {
            alteredUnits.forEach(entityRefreshUtilService::refresh);
            if (user.equals(mission.getTargetPlanet().getOwner())) {
                obtainedUnitEventEmitter.emitObtainedUnits(user);
                transactionUtilService.runWithRequiresNew(() ->
                        requirementBo.triggerUnitBuildCompletedOrKilled(user, alteredUnits.stream().map(ObtainedUnit::getUnit).toList())
                );
            }
            missionEventEmitterBo.emitLocalMissionChange(mission, user.getId());
        });
        return null;
    }
}
