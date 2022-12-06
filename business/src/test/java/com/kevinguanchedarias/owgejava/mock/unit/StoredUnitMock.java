package com.kevinguanchedarias.owgejava.mock.unit;

import com.kevinguanchedarias.owgejava.entity.jdbc.StoredUnit;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_ID;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_2_ID;

@UtilityClass
public class StoredUnitMock {
    public static final long STORED_UNIT_ID = 119;
    public static final long STORED_UNIT_OWNER_OU = OBTAINED_UNIT_1_ID;
    public static final long STORED_UNIT_TARGET_OU = OBTAINED_UNIT_2_ID;

    public static StoredUnit givenStoredUnit() {
        return StoredUnit.builder()
                .id(STORED_UNIT_ID)
                .ownerObtainedUnitId(STORED_UNIT_OWNER_OU)
                .targetObtainedUnitId(STORED_UNIT_TARGET_OU)
                .build();
    }
}
