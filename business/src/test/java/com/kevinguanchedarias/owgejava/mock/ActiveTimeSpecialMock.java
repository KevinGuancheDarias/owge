package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.givenTimeSpecial;

@UtilityClass
public class ActiveTimeSpecialMock {
    public static ActiveTimeSpecial givenActiveTimeSpecialMock(TimeSpecialStateEnum timeSpecialStateEnum) {
        return ActiveTimeSpecial.builder()
                .timeSpecial(givenTimeSpecial())
                .state(timeSpecialStateEnum)
                .user(UserMock.givenUser1())
                .build();
    }
}
