package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MissionRegistrationInvisibleManager {
    private final HiddenUnitBo hiddenUnitBo;
    private final MissionRepository missionRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;

    public void handleDefineMissionAsInvisible(Mission mission, List<ObtainedUnit> obtainedUnits) {
        mission.setInvisible(
                obtainedUnits.stream().allMatch(obtainedUnit -> hiddenUnitBo.isHiddenUnit(obtainedUnit.getUser(), obtainedUnit.getUnit()))
        );
    }

    public void maybeUpdateMissionsVisibility(List<Mission> missions) {
        missions.stream()
                .filter(current -> {
                    var oldValue = Boolean.TRUE.equals(current.getInvisible());
                    var newValue = obtainedUnitRepository.findByMissionId(current.getId()).stream()
                            .allMatch(obtainedUnit -> hiddenUnitBo.isHiddenUnit(obtainedUnit.getUser(), obtainedUnit.getUnit()));
                    current.setInvisible(newValue);
                    return oldValue != newValue;
                })
                .forEach(missionRepository::save);
    }
}
