package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.PlanetList;
import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;

@UtilityClass
public class PlanetListMock {
    public static final String PLANET_LIST_NAME = "PLANET_LIST_NAME";
    
    public static PlanetList givenPlanetList() {
        return PlanetList.builder()
                .planetUser(PlanetUser.builder().planet(givenSourcePlanet()).user(givenUser1()).build())
                .name(PLANET_LIST_NAME)
                .build();
    }
}
