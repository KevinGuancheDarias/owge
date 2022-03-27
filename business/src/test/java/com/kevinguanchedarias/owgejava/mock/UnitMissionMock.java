package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.SelectedUnit;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import lombok.experimental.UtilityClass;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.SOURCE_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;

@UtilityClass
public class UnitMissionMock {
    public static final long SELECTED_UNIT_COUNT = 721239;

    public static UnitMissionInformation givenUnitMissionInformation(MissionType missionType) {
        return UnitMissionInformation.builder()
                .userId(USER_ID_1)
                .sourcePlanetId(SOURCE_PLANET_ID)
                .targetPlanetId(TARGET_PLANET_ID)
                .missionType(missionType)
                .involvedUnits(List.of(givenSelectedUnit(null)))
                .build();
    }

    public static SelectedUnit givenSelectedUnit(Long expirationId) {
        return SelectedUnit.builder()
                .id(UNIT_ID_1)
                .count(SELECTED_UNIT_COUNT)
                .expirationId(expirationId)
                .build();
    }
}
