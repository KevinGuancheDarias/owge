package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UpgradeMock {
    public static Upgrade givenUpgrade(int id) {
        var upgrade = new Upgrade();
        upgrade.setId(id);
        return upgrade;
    }

    public static ObtainedUpgrade givenObtainedUpgrade(int id, UserStorage user) {
        return ObtainedUpgrade.builder()
                .upgrade(givenUpgrade(id))
                .userId(user)
                .build();
    }
}
