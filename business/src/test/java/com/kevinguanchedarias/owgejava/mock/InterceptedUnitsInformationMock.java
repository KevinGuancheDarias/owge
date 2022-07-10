package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import lombok.experimental.UtilityClass;

import java.util.Set;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;

@UtilityClass
public class InterceptedUnitsInformationMock {
    public static InterceptedUnitsInformation givenInterceptedUnitsInformation() {
        var unit1 = givenObtainedUnit1();
        var unit2 = givenObtainedUnit2();
        return InterceptedUnitsInformation.builder()
                .interceptedUnits(Set.of(unit1))
                .interceptorUser(unit2.getUser())
                .interceptorUnit(unit2)
                .build();
    }
}
