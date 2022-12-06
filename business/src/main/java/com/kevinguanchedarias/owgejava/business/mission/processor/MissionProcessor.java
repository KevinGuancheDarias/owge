package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;

import java.util.List;

public interface MissionProcessor {
    boolean supports(MissionType missionType);

    UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits);
}
