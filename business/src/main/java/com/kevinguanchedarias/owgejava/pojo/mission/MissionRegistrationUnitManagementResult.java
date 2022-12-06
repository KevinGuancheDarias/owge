package com.kevinguanchedarias.owgejava.pojo.mission;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MissionRegistrationUnitManagementResult {
    List<ObtainedUnit> units;
    List<Mission> alteredVisibilityMissions;
}
