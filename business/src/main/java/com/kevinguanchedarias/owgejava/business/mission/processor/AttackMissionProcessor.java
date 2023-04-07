package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.audit.AuditBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.attack.AttackMissionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class AttackMissionProcessor implements MissionProcessor {

    private final ObtainedUnitRepository obtainedUnitRepository;
    private final AttackMissionManagerBo attackMissionManagerBo;
    private final MissionReportManagerBo missionReportManagerBo;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final AuditBo auditBo;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final RequirementBo requirementBo;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.ATTACK;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        return processAttack(mission, true).getReportBuilder();
    }

    /**
     * @return True if should continue the mission
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public boolean triggerAttackIfRequired(Mission mission, UserStorage user, Planet targetPlanet) {
        boolean continueMission = true;
        if (attackMissionManagerBo.isAttackTriggerEnabledForMission(MissionType.valueOf(mission.getType().getCode()))
                && obtainedUnitRepository.areUnitsInvolved(user.getId(), user.getAlliance(), targetPlanet.getId())) {
            var result = processAttack(mission, false);
            continueMission = !result.isRemoved();
        }
        return continueMission;
    }

    public AttackInformation processAttack(Mission mission, boolean survivorsDoReturn) {
        var targetPlanet = mission.getTargetPlanet();
        AttackInformation attackInformation = attackMissionManagerBo.buildAttackInformation(targetPlanet, mission);
        attackMissionManagerBo.startAttack(attackInformation);
        if (survivorsDoReturn && !attackInformation.isRemoved()) {
            returnMissionRegistrationBo.registerReturnMission(mission, null);
        }
        mission.setResolved(true);
        UnitMissionReportBuilder builder = UnitMissionReportBuilder
                .create(mission.getUser(), mission.getSourcePlanet(), targetPlanet, List.of())
                .withAttackInformation(attackInformation);
        UserStorage invoker = mission.getUser();
        missionReportManagerBo.handleMissionReportSave(mission, builder, true,
                attackInformation.getUsers().values().stream().map(AttackUserInformation::getUser)
                        .filter(user -> !user.getId().equals(invoker.getId())).toList());
        attackInformation.getUsers().values().stream()
                .map(AttackUserInformation::getUser)
                .filter(user -> !mission.getUser().getId().equals(user.getId()))
                .forEach(user -> auditBo.nonRequestAudit(AuditActionEnum.ATTACK_INTERACTION, null, mission.getUser(), user.getId()));
        var owner = targetPlanet.getOwner();
        if (attackInformation.isRemoved() || (owner != null && !attackInformation.getUsersWithDeletedMissions().isEmpty())) {
            missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission); // Maybe useless?, should test
        }
        attackInformation.getUnits().stream().distinct().forEach(this::triggerUnitRequirementChange);
        attackInformation.setReportBuilder(builder);
        return attackInformation;
    }

    private void triggerUnitRequirementChange(AttackObtainedUnit attackObtainedUnit) {
        var user = attackObtainedUnit.getObtainedUnit().getUser();
        var unit = attackObtainedUnit.getObtainedUnit().getUnit();
        if (attackObtainedUnit.getFinalCount().equals(0L)) {
            requirementBo.triggerUnitBuildCompletedOrKilled(user, unit);
        } else if (!attackObtainedUnit.getFinalCount().equals(attackObtainedUnit.getInitialCount())) {
            requirementBo.triggerUnitAmountChanged(user, unit);
        }

    }
}
