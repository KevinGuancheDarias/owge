package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SpeedImpactGroupMock {
    public static final int SPEED_IMPACT_GROUP_ID = 982;

    public static SpeedImpactGroup givenSpeedImpactGroup(int id) {
        return SpeedImpactGroup.builder()
                .id(id)
                .build();
    }

    public static SpeedImpactGroup givenSpeedImpactGroup() {
        return givenSpeedImpactGroup(SPEED_IMPACT_GROUP_ID);
    }
}
