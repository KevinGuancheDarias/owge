package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.pojo.UnitTypesOverride;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FactionMock {
    public static Faction givenEntity() {
        return Faction.builder()
                .id(1)
                .improvement(ImprovementMock.givenEntity())
                .build();
    }

    public static UnitTypesOverride givenOverride(int overrideId, long maxCount) {
        UnitTypesOverride override = new UnitTypesOverride();
        override.setId(overrideId);
        override.setOverrideMaxCount(maxCount);
        return override;
    }
}
