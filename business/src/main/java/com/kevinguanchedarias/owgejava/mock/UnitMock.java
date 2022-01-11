package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Unit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UnitMock {
    public static final int UNIT_ID = 1223;
    public static final String UNIT_NAME = "Paco";
    
    public static Unit givenUnit() {
        var retVal = new Unit();
        retVal.setId(UNIT_ID);
        retVal.setName(UNIT_NAME);
        return retVal;
    }
}
