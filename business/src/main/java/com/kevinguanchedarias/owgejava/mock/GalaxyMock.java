package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Galaxy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GalaxyMock {
    public static final int GALAXY_ID = 18;

    public static Galaxy givenGalaxy() {
        return givenGalaxy(GALAXY_ID);
    }

    public static Galaxy givenGalaxy(int id) {
        return Galaxy.builder()
                .id(id)
                .build();
    }
}
