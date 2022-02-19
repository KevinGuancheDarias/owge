package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;

@UtilityClass
public class InterceptableSpeedGroupMock {
    public static InterceptableSpeedGroup givenInterceptableSpeedGroup() {
        return InterceptableSpeedGroup.builder()
                .speedImpactGroup(givenSpeedImpactGroup())
                .build();
    }
}
