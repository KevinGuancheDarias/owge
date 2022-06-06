package com.kevinguanchedarias.owgejava.enumerations;

public enum RequirementTypeEnum {
    HAVE_SPECIAL_LOCATION(1), HAVE_UNIT(2), BEEN_RACE(3), UPGRADE_LEVEL(4),
    WORST_PLAYER(5), UNIT_AMOUNT(6), HOME_GALAXY(7), HAVE_SPECIAL_AVAILABLE(8),
    HAVE_SPECIAL_ENABLED(9), UPGRADE_LEVEL_LOWER_THAN(10), UNIMPLEMENTED_PLACEHOLDER(-1);

    private final int value;

    RequirementTypeEnum(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}