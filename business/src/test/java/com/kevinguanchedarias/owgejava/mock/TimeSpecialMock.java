package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import lombok.experimental.UtilityClass;

import java.util.Date;

@UtilityClass
public class TimeSpecialMock {
    public static final int TIME_SPECIAL_ID = 4;
    public static final long ACTIVE_TIME_SPECICAL_ID = 783671;
    public static final long TIME_SPECIAL_DURATION = 9482;
    public static final long SECOND_VALUE = 19;
    public static final long ACTIVE_TIME_SPECIAL_RECHARGE_TIME = 86400;

    public static TimeSpecial givenTimeSpecial() {
        var instance = new TimeSpecial();
        instance.setId(TIME_SPECIAL_ID);
        instance.setDuration(TIME_SPECIAL_DURATION);
        instance.setRechargeTime(ACTIVE_TIME_SPECIAL_RECHARGE_TIME);
        return instance;
    }

    public static ActiveTimeSpecial givenActiveTimeSpecial() {
        return ActiveTimeSpecial.builder()
                .id(ACTIVE_TIME_SPECICAL_ID)
                .timeSpecial(givenTimeSpecial())
                .readyDate(new Date())
                .build();
    }
}
