package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import com.kevinguanchedarias.owgejava.pojo.mission.MissionInterceptionInformation;
import lombok.experimental.UtilityClass;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.InterceptedUnitsInformationMock.givenInterceptedUnitsInformation;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;

@UtilityClass
public class MissionInterceptionInformationMock {
    public static final boolean INTERCEPTION_INFORMATION_IS_INTERCEPTED = true;
    public static final int INTERCEPTION_INFORMATION_TOTAL = 2;
    public static final List<ObtainedUnit> INTERCEPTION_INFORMATION_INVOLVED = List.of(givenObtainedUnit1());
    public static final List<ObtainedUnit> INTERCEPTION_INFORMATION_ORIGINALLY = List.of(givenObtainedUnit2());
    public static final List<InterceptedUnitsInformation> INTERCEPTION_INFORMATION_INTERCEPTED = List.of(givenInterceptedUnitsInformation());
    
    public static MissionInterceptionInformation givenMissionInterceptionInformation() {
        return MissionInterceptionInformation.builder()
                .isMissionIntercepted(INTERCEPTION_INFORMATION_IS_INTERCEPTED)
                .totalInterceptedUnits(INTERCEPTION_INFORMATION_TOTAL)
                .involvedUnits(INTERCEPTION_INFORMATION_INVOLVED)
                .originallyInvolved(INTERCEPTION_INFORMATION_ORIGINALLY)
                .interceptedUnits(INTERCEPTION_INFORMATION_INTERCEPTED)
                .build();
    }
}
