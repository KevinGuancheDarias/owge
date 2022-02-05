package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.MissionType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MissionTypeMock {
    public static MissionType givenMissionTypeDeployed() {
        return MissionType.builder()
                .code(com.kevinguanchedarias.owgejava.enumerations.MissionType.DEPLOYED.name())
                .build();
    }
}
