package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Galaxy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GalaxyMock {
    public static final int GALAXY_ID = 18;
    public static final String GALAXY_NAME = "Via TestBug";

    public static Galaxy givenGalaxy() {
        return givenGalaxy(GALAXY_ID);
    }

    public static Galaxy givenGalaxy(int id) {
        return Galaxy.builder()
                .id(id)
                .name(GALAXY_NAME)
                .sectors(5L)
                .quadrants(5L)
                .numPlanets(20L)
                .build();
    }

    public static Galaxy givenGalaxy(long sectors, long quadrants, long numPlanets) {
        return givenGalaxy().toBuilder()
                .sectors(sectors)
                .quadrants(quadrants)
                .numPlanets(numPlanets)
                .build();
    }
}
