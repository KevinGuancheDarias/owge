package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.MissionType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MissionTypeMock {
    public static MissionType givenMissinType(com.kevinguanchedarias.owgejava.enumerations.MissionType type) {
        return MissionType.builder()
                .code(type.name())
                .build();
    }
}
