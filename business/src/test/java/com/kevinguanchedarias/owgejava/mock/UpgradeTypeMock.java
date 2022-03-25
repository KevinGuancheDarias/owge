package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.UpgradeType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UpgradeTypeMock {
    public static final int UPGRADE_TYPE_ID = 6191;

    public static UpgradeType givenUpgradeType() {
        return UpgradeType.builder()
                .id(UPGRADE_TYPE_ID)
                .build();
    }
}
