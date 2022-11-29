package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ConquestMissionProcessor implements MissionProcessor {
    private final PlanetBo planetBo;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final AttackMissionProcessor attackMissionProcessor;
    private final RequirementBo requirementBo;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MissionReportManagerBo missionReportManagerBo;
    private final MissionBo missionBo;
    private final MissionRepository missionRepository;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.CONQUEST;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        UserStorage user = mission.getUser();
        Planet targetPlanet = mission.getTargetPlanet();
        UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
                targetPlanet, involvedUnits);
        boolean maxPlanets = planetBo.hasMaxPlanets(user);
        boolean areUnitsHavingToReturn = false;
        AttackInformation attackInformation = attackMissionProcessor.processAttack(mission, false);
        UserStorage oldOwner = targetPlanet.getOwner();
        boolean isOldOwnerDefeated;
        boolean isAllianceDefeated;
        if (oldOwner == null) {
            isOldOwnerDefeated = true;
            isAllianceDefeated = true;
        } else {
            isOldOwnerDefeated = calculateIsOldOwnerDefeated(attackInformation, oldOwner);
            isAllianceDefeated = calculateIsAllianceDefeated(attackInformation, oldOwner, isOldOwnerDefeated);
        }

        if (isFailedConquest(targetPlanet, maxPlanets, isOldOwnerDefeated, isAllianceDefeated)) {
            if (!attackInformation.isRemoved()) {
                returnMissionRegistrationBo.registerReturnMission(mission, null);
                areUnitsHavingToReturn = true;
            }
            appendConquestInformation(builder, maxPlanets, isOldOwnerDefeated, isAllianceDefeated);
        } else {
            planetBo.definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
            builder.withConquestInformation(true, "I18N_PLANET_IS_NOW_OURS");
            if (targetPlanet.getSpecialLocation() != null && oldOwner != null) {
                requirementBo.triggerSpecialLocation(oldOwner, targetPlanet.getSpecialLocation());
            }
            if (oldOwner != null) {
                planetBo.emitPlanetOwnedChange(oldOwner);
                findUnitBuildMission(targetPlanet).ifPresent(missionBo::adminCancelBuildMission);
                missionEventEmitterBo.emitEnemyMissionsChange(oldOwner);
                UnitMissionReportBuilder enemyReportBuilder = UnitMissionReportBuilder
                        .create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
                        .withConquestInformation(true, "I18N_YOUR_PLANET_WAS_CONQUISTED");
                missionReportManagerBo.handleMissionReportSave(mission, enemyReportBuilder, true, oldOwner);
            }

        }
        mission.setResolved(true);
        if (!areUnitsHavingToReturn) {
            missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission);
        }
        return builder;
    }

    private boolean calculateIsAllianceDefeated(AttackInformation attackInformation, UserStorage oldOwner, boolean isOldOwnerDefeated) {
        return isOldOwnerDefeated
                && (oldOwner.getAlliance() == null || attackInformation.getUsers().entrySet().stream()
                .filter(attackedUser -> attackedUser.getValue().getUser().getAlliance() != null
                        && attackedUser.getValue().getUser().getAlliance().equals(oldOwner.getAlliance()))
                .allMatch(currentUser -> currentUser.getValue().getUnits().stream()
                        .noneMatch(currentUserUnit -> currentUserUnit.getFinalCount() > 0L)));
    }

    private boolean calculateIsOldOwnerDefeated(AttackInformation attackInformation, UserStorage oldOwner) {
        return !attackInformation.getUsers().containsKey(oldOwner.getId())
                || attackInformation.getUsers().get(oldOwner.getId()).getUnits().stream()
                .noneMatch(current -> current.getFinalCount() > 0L);
    }

    private boolean isFailedConquest(Planet targetPlanet, boolean maxPlanets, boolean isOldOwnerDefeated, boolean isAllianceDefeated) {
        return !isOldOwnerDefeated || !isAllianceDefeated || maxPlanets || planetBo.isHomePlanet(targetPlanet);
    }

    private void appendConquestInformation(
            UnitMissionReportBuilder builder, boolean maxPlanets, boolean isOldOwnerDefeated, boolean isAllianceDefeated
    ) {
        if (maxPlanets) {
            builder.withConquestInformation(false, "FOO");
        } else if (!isOldOwnerDefeated) {
            builder.withConquestInformation(false, "I18N_OWNER_NOT_DEFEATED");
        } else if (!isAllianceDefeated) {
            builder.withConquestInformation(false, "I18N_ALLIANCE_NOT_DEFEATED");
        } else {
            builder.withConquestInformation(false, "I18N_CANT_CONQUER_HOME_PLANET");
        }
    }

    private Optional<Mission> findUnitBuildMission(Planet planet) {
        return missionRepository.findOneByResolvedFalseAndTypeCodeAndMissionInformationValue(MissionType.BUILD_UNIT.name(), planet.getId().doubleValue());
    }
}
