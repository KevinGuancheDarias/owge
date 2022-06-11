package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Planet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanetMock {
    public static final long SOURCE_PLANET_ID = 111;
    public static final long TARGET_PLANET_ID = 121;

    public static Planet givenSourcePlanet() {
        var planet = new Planet();
        planet.setId(SOURCE_PLANET_ID);
        planet.setGalaxy(GalaxyMock.givenGalaxy());
        return planet;
    }

    public static Planet givenTargetPlanet() {
        var planet = new Planet();
        planet.setId(TARGET_PLANET_ID);
        planet.setGalaxy(GalaxyMock.givenGalaxy());
        return planet;
    }
}
