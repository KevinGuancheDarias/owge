package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.UnitType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UnitTypeMock {
    public static final int UNIT_TYPE_ID = 91;


    public static UnitType givenUnitType() {
        return givenUnitType(UNIT_TYPE_ID);
    }

    public static UnitType givenUnitType(int id) {
        UnitType unitType = new UnitType();
        unitType.setId(id);
        return unitType;
    }


}
