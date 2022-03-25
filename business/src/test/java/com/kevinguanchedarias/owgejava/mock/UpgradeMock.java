package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Upgrade;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UpgradeMock {
    public static final int UPGRADE_ID = 2763;

    public static Upgrade givenUpgrade() {
        return givenUpgrade(UPGRADE_ID);
    }

    public static Upgrade givenUpgrade(int id) {
        var upgrade = new Upgrade();
        upgrade.setId(id);
        return upgrade;
    }
}
