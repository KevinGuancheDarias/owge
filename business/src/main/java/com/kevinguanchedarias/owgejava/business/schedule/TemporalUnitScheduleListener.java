package com.kevinguanchedarias.owgejava.business.schedule;

import com.kevinguanchedarias.owgejava.business.ScheduledTasksManagerService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.requirement.listener.RequirementComplianceListener;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.business.util.UnitImprovementUtilService;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.entity.jdbc.ObtainedUnitTemporalInformation;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import com.kevinguanchedarias.owgejava.util.SetUtils;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveTemporalUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID;

@Service
@AllArgsConstructor
@Slf4j
public class TemporalUnitScheduleListener implements RequirementComplianceListener {
    public static final String TASK_NAME = "UNIT_EXPIRED";

    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ScheduledTasksManagerService scheduledTasksManagerService;
    private final PlanetLockUtilService planetLockUtilService;
    private final TransactionUtilService transactionUtilService;
    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;
    private final MissionRepository missionRepository;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final TaggableCacheManager taggableCacheManager;
    private final UnitImprovementUtilService unitImprovementUtilService;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final RuleBo ruleBo;
    private final ObjectRelationsRepository objectRelationsRepository;

    @PostConstruct
    public void init() {
        scheduledTasksManagerService.addHandler(TASK_NAME, task -> {
            log.debug("Deleting expired unit: {}", task.getContent());
            var expirationId = ((Double) task.getContent()).longValue();
            if (obtainedUnitTemporalInformationRepository.existsById(expirationId)) {
                aggressiveLockAcquire(expirationId, () ->
                        transactionUtilService.runWithRequired(() -> doDeleteExpiredOrOrDemand(expirationId))
                );
            }
        });
    }

    @Override
    public void relationLost(UnlockedRelation unlockedRelation) {
        var timeSpecialId = unlockedRelation.getRelation().getReferenceId();
        if (ObjectEnum.TIME_SPECIAL.isObject(unlockedRelation.getRelation().getObject())) {
            activeTimeSpecialRepository.findOneByTimeSpecialIdAndUserId(timeSpecialId, unlockedRelation.getUser().getId())
                    .filter(ats -> TimeSpecialStateEnum.ACTIVE.equals(ats.getState()))
                    .ifPresent(this::doDeactivateTimeSpecial);
        }
    }

    private void doDeactivateTimeSpecial(ActiveTimeSpecial ats) {
        var objectCode = ObjectEnum.TIME_SPECIAL.name();
        var timeSpecialId = ats.getTimeSpecial().getId();
        ruleBo.findByOriginTypeAndOriginIdAndType(objectCode, ats.getTimeSpecial().getId(), TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID).stream()
                .filter(ruleDto -> ruleDto.getExtraArgs().size() == 2 && ObjectEnum.UNIT.name().equals(ruleDto.getDestinationType()))
                .findFirst()
                .map(ruleDto -> objectRelationsRepository.findOneByObjectCodeAndReferenceId(objectCode, timeSpecialId))
                .map(ObjectRelation::getId)
                .map(obtainedUnitTemporalInformationRepository::findByRelationId)
                .ifPresent(this::deleteOnDemand);
    }

    private void deleteOnDemand(List<ObtainedUnitTemporalInformation> obtainedUnitTemporalInformationList) {
        obtainedUnitTemporalInformationList.forEach(entry -> deleteOnDemand(entry.getId()));
    }

    private void deleteOnDemand(long expirationId) {
        aggressiveLockAcquire(expirationId, () -> doDeleteExpiredOrOrDemand(expirationId));
    }

    private void doDeleteExpiredOrOrDemand(long expirationId) {
        var ouList = obtainedUnitRepository.findByExpirationId(expirationId);
        obtainedUnitRepository.deleteAll(ouList);
        if (!ouList.isEmpty()) {
            var user = ouList.get(0).getUser();
            unitImprovementUtilService.maybeTriggerClearImprovement(user, ouList);
            obtainedUnitEventEmitter.emitObtainedUnitsAfterCommit(user);
            handleAffectedMissions(affectedMissions(ouList));
        }
        obtainedUnitTemporalInformationRepository.deleteById(expirationId);
    }

    private void aggressiveLockAcquire(long expirationId, Runnable runnable) {
        var planetIds = obtainedUnitRepository.findPlanetIdsByExpirationId(expirationId);
        if (!planetIds.isEmpty()) {
            planetLockUtilService.doInsideLockById(planetIds.stream().toList(), () -> {
                var innerPlanetIds = obtainedUnitRepository.findPlanetIdsByExpirationId(expirationId);
                if (innerPlanetIds.equals(planetIds)) {
                    runnable.run();
                } else {
                    aggressiveLockAcquire(expirationId, runnable);
                }
            });
        }
    }

    private void handleAffectedMissions(Set<Mission> missions) {
        if (!missions.isEmpty()) {
            @SuppressWarnings("ConstantConditions") var user = SetUtils.getFirstElement(missions).getUser();
            taggableCacheManager.evictByCacheTag(Mission.MISSION_BY_USER_CACHE_TAG, user.getId());
            var affectedUsers = usersOwningPlanetsOfTargetMissions(user, missions);
            deleteNonUnitsLeftMissions(missions);
            transactionUtilService.doAfterCommit(() -> {
                missionEventEmitterBo.emitUnitMissions(user.getId());
                missionEventEmitterBo.emitMissionCountChange(user.getId());
            });
            affectedUsers.forEach(missionEventEmitterBo::emitEnemyMissionsChange);
        }
    }

    private Set<UserStorage> usersOwningPlanetsOfTargetMissions(UserStorage temporalUnitOwner, Set<Mission> missions) {
        return missions.stream()
                .map(Mission::getTargetPlanet)
                .filter(Objects::nonNull)
                .map(Planet::getOwner)
                .filter(planetOwner -> planetOwner != null && !planetOwner.equals(temporalUnitOwner))
                .collect(Collectors.toUnmodifiableSet());
    }

    private void deleteNonUnitsLeftMissions(Set<Mission> missions) {
        missions.stream()
                .filter(mission -> !obtainedUnitRepository.existsByMission(mission))
                .forEach(missionRepository::delete);
    }

    private Set<Mission> affectedMissions(List<ObtainedUnit> ouList) {
        return ouList.stream()
                .map(ObtainedUnit::getMission)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

}
