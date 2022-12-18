package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SpecialLocationMock {
    public static final int SPECIAL_LOCATION_ID = 481134;

    public static SpecialLocation givenSpecialLocation() {
        var retVal = new SpecialLocation();
        retVal.setId(SPECIAL_LOCATION_ID);
        return retVal;
    }
}
