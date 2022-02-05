package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeSpecialMock {
    public static final int TIME_SPECIAL_ID = 4;

    public static TimeSpecial givenTimeSpecial() {
        var instance = new TimeSpecial();
        instance.setId(TIME_SPECIAL_ID);
        return instance;
    }
}
