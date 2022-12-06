package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class CounterattackMissionProcessor implements MissionProcessor {
    private final AttackMissionProcessor attackMissionProcessor;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.COUNTERATTACK;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        return attackMissionProcessor.process(mission, involvedUnits);
    }
}
