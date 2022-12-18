package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Planet;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.givenGalaxy;

@UtilityClass
public class PlanetMock {
    public static final long SOURCE_PLANET_ID = 111;
    public static final long TARGET_PLANET_ID = 121;
    public static final long PLANET_SECTOR = 14;
    public static final long PLANET_QUADRANT = 1020;
    public static final int PLANET_NUMBER = 18;

    public static Planet givenSourcePlanet() {
        var planet = new Planet();
        planet.setId(SOURCE_PLANET_ID);
        setCommon(planet);
        return planet;
    }

    public static Planet givenPlanet(long id) {
        var planet = new Planet();
        planet.setId(id);
        setCommon(planet);
        return planet;
    }

    public static Planet givenTargetPlanet() {
        var planet = new Planet();
        planet.setId(TARGET_PLANET_ID);
        setCommon(planet);
        return planet;
    }

    private static void setCommon(Planet planet) {
        planet.setGalaxy(givenGalaxy());
        planet.setQuadrant(PLANET_QUADRANT);
        planet.setSector(PLANET_SECTOR);
        planet.setPlanetNumber(PLANET_NUMBER);
    }
}
