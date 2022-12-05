package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class MissionRegistrationOrphanMissionEraser {
    private final ObtainedUnitRepository obtainedUnitRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void doMarkAsDeletedTheOrphanMissions(Set<Mission> deletedMissions) {
        List<ObtainedUnit> unitsInMissionsAfterDelete = obtainedUnitRepository
                .findByMissionIdIn(deletedMissions.stream().map(Mission::getId).toList());
        deletedMissions.stream().filter(mission -> unitsInMissionsAfterDelete.stream()
                .noneMatch(unit -> mission.getId().equals(unit.getMission() != null ? unit.getMission().getId() : null))

        ).forEach(mission -> mission.setResolved(true));
    }

}
