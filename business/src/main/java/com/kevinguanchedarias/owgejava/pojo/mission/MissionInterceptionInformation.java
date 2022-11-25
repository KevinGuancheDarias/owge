package com.kevinguanchedarias.owgejava.pojo.mission;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MissionInterceptionInformation {
    boolean isMissionIntercepted;
    int totalInterceptedUnits;
    List<ObtainedUnit> involvedUnits;
    List<ObtainedUnit> originallyInvolved;
    List<InterceptedUnitsInformation> interceptedUnits;
}
