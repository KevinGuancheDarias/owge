package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Upgrade;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.UpgradeTypeMock.givenUpgradeType;

@UtilityClass
public class UpgradeMock {
    public static final int UPGRADE_ID = 2763;

    public static Upgrade givenUpgrade() {
        return givenUpgrade(UPGRADE_ID);
    }

    public static Upgrade givenUpgrade(int id) {
        var upgrade = new Upgrade();
        upgrade.setId(id);
        upgrade.setType(givenUpgradeType());
        return upgrade;
    }
}
