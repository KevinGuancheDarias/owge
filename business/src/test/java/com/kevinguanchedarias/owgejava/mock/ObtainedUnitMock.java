package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.projection.ObtainedUnitBasicInfoProjection;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.ImageStoreMock.givenImageStore;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_NAME;

@UtilityClass
public class ObtainedUnitMock {
    public static final long OBTAINED_UNIT_1_ID = 1929;
    public static final long OBTAINED_UNIT_2_ID = 192921;
    public static final long OBTAINED_UNIT_BYPASS_SHIELD_ID = 191282;
    public static final long OBTAINED_UNIT_1_COUNT = 10;
    public static final long OBTAINED_UNIT_2_COUNT = 8;

    public static ObtainedUnit givenObtainedUnit1() {
        var ou = new ObtainedUnit();
        ou.setId(OBTAINED_UNIT_1_ID);
        ou.setUnit(UnitMock.givenUnit1());
        ou.setUser(UserMock.givenUser1());
        ou.setSourcePlanet(PlanetMock.givenSourcePlanet());
        ou.setTargetPlanet(PlanetMock.givenTargetPlanet());
        ou.setCount(OBTAINED_UNIT_1_COUNT);
        return ou;
    }

    public static ObtainedUnit givenObtainedUnit2() {
        var ou = new ObtainedUnit();
        ou.setId(OBTAINED_UNIT_2_ID);
        ou.setUnit(UnitMock.givenUnit2());
        ou.setUser(UserMock.givenUser2());
        ou.setSourcePlanet(PlanetMock.givenSourcePlanet());
        ou.setTargetPlanet(PlanetMock.givenTargetPlanet());
        ou.setCount(OBTAINED_UNIT_2_COUNT);
        return ou;
    }

    public static ObtainedUnitBasicInfoProjection givenObtainedUnitBasicInfoProjection() {
        return ObtainedUnitBasicInfoProjection.builder()
                .id(OBTAINED_UNIT_2_ID)
                .count(OBTAINED_UNIT_2_COUNT)
                .unitName(UNIT_NAME)
                .unitImage(givenImageStore())
                .build();
    }

    public static ObtainedUnit givenObtainedUnitWithBypassShields(UserStorage user) {
        var ou = givenObtainedUnit2();
        ou.setId(OBTAINED_UNIT_BYPASS_SHIELD_ID);
        ou.setUnit(UnitMock.givenUnitBypassShields());
        ou.setUser(user);
        return ou;
    }
}
