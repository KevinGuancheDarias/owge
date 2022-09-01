package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import lombok.experimental.UtilityClass;

import java.util.Set;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;

@UtilityClass
public class InterceptedUnitsInformationMock {
    public static final ObtainedUnit INTERCEPTED_UNIT = givenObtainedUnit1();
    public static final ObtainedUnit INTERCEPTOR_UNIT = givenObtainedUnit2();
    public static final UserStorage INTERCEPTOR_USER = INTERCEPTOR_UNIT.getUser();

    public static InterceptedUnitsInformation givenInterceptedUnitsInformation() {
        return givenInterceptedUnitsInformation(Set.of(INTERCEPTED_UNIT));
    }

    public static InterceptedUnitsInformation givenInterceptedUnitsInformation(Set<ObtainedUnit> interceptedUnits) {
        return InterceptedUnitsInformation.builder()
                .interceptedUnits(interceptedUnits)
                .interceptorUser(INTERCEPTOR_USER)
                .interceptorUnit(INTERCEPTOR_UNIT)
                .build();
    }
}
