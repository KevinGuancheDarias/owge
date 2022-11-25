package com.kevinguanchedarias.owgejava.business.schedule;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.ScheduledTasksManagerService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import com.kevinguanchedarias.owgejava.util.SetUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TemporalUnitScheduleListener {
    public static final String TASK_NAME = "UNIT_EXPIRED";

    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ScheduledTasksManagerService scheduledTasksManagerService;
    private final PlanetLockUtilService planetLockUtilService;
    private final TransactionUtilService transactionUtilService;
    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;
    private final MissionRepository missionRepository;
    private final MissionBo missionBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;

    @PostConstruct
    public void init() {
        scheduledTasksManagerService.addHandler(TASK_NAME, task -> {
            var expirationId = ((Double) task.getContent()).longValue();
            aggressiveLockAcquire(expirationId, () ->
                    transactionUtilService.runWithRequired(() -> {
                        var ouList = obtainedUnitRepository.findByExpirationId(expirationId);
                        obtainedUnitRepository.deleteAll(ouList);
                        if (!ouList.isEmpty()) {
                            obtainedUnitEventEmitter.emitObtainedUnits(ouList.get(0).getUser());
                            handleAffectedMissions(affectedMissions(ouList));
                        }
                        obtainedUnitTemporalInformationRepository.deleteById(expirationId);
                    })
            );
        });
    }

    private void aggressiveLockAcquire(long expirationId, Runnable runnable) {
        var planetIds = obtainedUnitRepository.findPlanetIdsByExpirationId(expirationId);
        planetLockUtilService.doInsideLockById(planetIds.stream().toList(), () -> {
            var innerPlanetIds = obtainedUnitRepository.findPlanetIdsByExpirationId(expirationId);
            if (innerPlanetIds.equals(planetIds)) {
                runnable.run();
            } else {
                aggressiveLockAcquire(expirationId, runnable);
            }
        });
    }

    private void handleAffectedMissions(Set<Mission> missions) {
        if (!missions.isEmpty()) {
            @SuppressWarnings("ConstantConditions") var user = SetUtils.getFirstElement(missions).getUser();
            var affectedUsers = usersOwningPlanetsOfTargetMissions(user, missions);
            deleteNonUnitsLeftMissions(missions);
            missionEventEmitterBo.emitUnitMissions(user.getId());
            missionBo.emitMissionCountChange(user.getId());
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
