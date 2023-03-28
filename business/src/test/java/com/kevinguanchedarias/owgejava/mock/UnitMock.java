package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Unit;
import lombok.experimental.UtilityClass;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.InterceptableSpeedGroupMock.givenInterceptableSpeedGroup;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenUnitType;

@UtilityClass
public class UnitMock {
    public static final int UNIT_ID_1 = 1223;
    public static final int UNIT_ID_2 = 122312;
    public static final int UNIT_OVER_WEIGHT_ID = 99;
    public static final int UNIT_BYPASS_SHIELDS_ID_3 = 122312;

    public static final String UNIT_NAME = "Paco";
    public static final int UNIT_POINTS_1 = 4;
    public static final int UNIT_POINTS_2 = 8;

    public static final int UNIT_ATTACK = 18;
    public static final int UNIT_SHIELD = 28;
    public static final int UNIT_HEALTH = 38;
    public static final double UNIT_SPEED = 49;

    public static Unit givenUnit1() {
        var retVal = new Unit();
        retVal.setId(UNIT_ID_1);
        retVal.setName(UNIT_NAME);
        retVal.setType(givenUnitType());
        retVal.setPoints(UNIT_POINTS_1);
        retVal.setAttack(UNIT_ATTACK);
        retVal.setShield(UNIT_SHIELD);
        retVal.setHealth(UNIT_HEALTH);
        retVal.setSpeed(UNIT_SPEED);
        retVal.setStorageCapacity(1L);
        return retVal;
    }

    public static Unit givenUnitWithInterception1() {
        var unit = givenUnit1();
        unit.setInterceptableSpeedGroups(List.of(givenInterceptableSpeedGroup()));
        return unit;
    }

    public static Unit givenUnit2() {
        var retVal = new Unit();
        retVal.setId(UNIT_ID_2);
        retVal.setName(UNIT_NAME);
        retVal.setType(givenUnitType());
        retVal.setPoints(UNIT_POINTS_2);
        retVal.setStorageCapacity(1L);
        return retVal;
    }

    public static Unit givenOverweightUnit() {
        var retVal = new Unit();
        retVal.setId(UNIT_OVER_WEIGHT_ID);
        retVal.setName(UNIT_NAME);
        retVal.setType(givenUnitType());
        retVal.setPoints(UNIT_POINTS_2);
        retVal.setStorageCapacity(1L);
        retVal.setStoredWeight(Integer.MAX_VALUE / 2);
        return retVal;
    }

    public static Unit givenUnitBypassShields() {
        var retVal = givenUnit2();
        retVal.setId(UNIT_BYPASS_SHIELDS_ID_3);
        retVal.setBypassShield(true);
        return retVal;
    }
}
