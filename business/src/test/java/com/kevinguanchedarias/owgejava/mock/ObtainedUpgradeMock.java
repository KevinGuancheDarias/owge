package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;

@UtilityClass
public class ObtainedUpgradeMock {
    public static final long OBTAINED_UPGRADE_ID = 871;
    public static final int OBTAINED_UPGRADE_LEVEL = 4;

    public static ObtainedUpgrade givenObtainedUpgrade() {
        return ObtainedUpgrade.builder()
                .id(OBTAINED_UPGRADE_ID)
                .upgrade(UpgradeMock.givenUpgrade())
                .user(givenUser1())
                .level(OBTAINED_UPGRADE_LEVEL)
                .build();
    }

    public static ObtainedUpgrade givenObtainedUpgrade(int id, UserStorage user) {
        return ObtainedUpgrade.builder()
                .upgrade(UpgradeMock.givenUpgrade(id))
                .user(user)
                .build();
    }
}
