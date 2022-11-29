package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionUnitsFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class DeployMissionProcessor implements MissionProcessor {
    private final MissionUnitsFinderBo missionUnitsFinderBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final TransactionUtilService transactionUtilService;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final EntityManager entityManager;
    private final HiddenUnitBo hiddenUnitBo;

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
        List<ObtainedUnit> alteredUnits = new ArrayList<>();
        missionUnitsFinderBo.findUnitsInvolved(missionId).forEach(current ->
                alteredUnits.add(obtainedUnitBo.moveUnit(current, userId, mission.getTargetPlanet().getId()))
        );

        var deployedMission = alteredUnits.get(0).getMission();
        if (deployedMission != null) {
            deployedMission.setInvisible(deployedMission.getInvolvedUnits().stream().allMatch(hiddenUnitBo::isHiddenUnit));
        }

        mission.setResolved(true);
        transactionUtilService.doAfterCommit(() -> {
            alteredUnits.forEach(entityManager::refresh);
            if (user.equals(mission.getTargetPlanet().getOwner())) {
                obtainedUnitEventEmitter.emitObtainedUnits(user);
            }
            missionEventEmitterBo.emitLocalMissionChange(mission, user.getId());
        });
        return null;
    }
}
