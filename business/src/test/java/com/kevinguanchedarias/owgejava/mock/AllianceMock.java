package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.AllianceJoinRequest;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;

@UtilityClass
public class AllianceMock {
    public static int ALLIANCE_ID = 41123;
    public static String ALLIANCE_NAME = "FooAlliance";
    public static String ALLIANCE_DESCRIPTION = "AllianceDescription";
    public static int ALLIANCE_JOIN_REQUEST_ID = 55712;

    public static Alliance givenAlliance() {
        return givenAlliance(ALLIANCE_ID);
    }

    public static Alliance givenAlliance(int allianceId) {
        var retVal = new Alliance();
        retVal.setId(allianceId);
        retVal.setName(ALLIANCE_NAME);
        retVal.setDescription(ALLIANCE_DESCRIPTION);
        return retVal;
    }

    public AllianceJoinRequest givenAllianceJoinRequest() {
        return AllianceJoinRequest.builder()
                .id(ALLIANCE_JOIN_REQUEST_ID)
                .alliance(givenAlliance())
                .user(givenUser1())
                .build();
    }
}
