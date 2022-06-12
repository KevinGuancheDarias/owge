package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Galaxy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GalaxyMock {
    public static final int GALAXY_ID = 18;

    public static Galaxy givenGalaxy() {
        return Galaxy.builder()
                .id(GALAXY_ID)
                .build();
    }
}
