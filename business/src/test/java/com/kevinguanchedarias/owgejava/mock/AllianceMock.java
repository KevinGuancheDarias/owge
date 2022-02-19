package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AllianceMock {
    public static int ALLIANCE_ID = 41123;

    public static Alliance givenAlliance() {
        return givenAlliance(ALLIANCE_ID);
    }

    public static Alliance givenAlliance(int allianceId) {
        var retVal = new Alliance();
        retVal.setId(allianceId);
        return retVal;
    }
}
