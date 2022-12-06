package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MissionRegistrationPlanetExistsChecker.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(PlanetRepository.class)
class MissionRegistrationPlanetExistsCheckerTest {
    private final MissionRegistrationPlanetExistsChecker missionRegistrationPlanetExistsChecker;
    private final PlanetRepository planetRepository;

    @Autowired
    MissionRegistrationPlanetExistsCheckerTest(
            MissionRegistrationPlanetExistsChecker missionRegistrationPlanetExistsChecker,
            PlanetRepository planetRepository
    ) {
        this.missionRegistrationPlanetExistsChecker = missionRegistrationPlanetExistsChecker;
        this.planetRepository = planetRepository;
    }

    @Test
    void checkPlanetExists_should_throw() {
        assertThatThrownBy(() -> missionRegistrationPlanetExistsChecker.checkPlanetExists(TARGET_PLANET_ID))
                .isInstanceOf(PlanetNotFoundException.class);
    }

    @Test
    void checkPlanetExists_should_work() {
        given(planetRepository.existsById(TARGET_PLANET_ID)).willReturn(true);

        missionRegistrationPlanetExistsChecker.checkPlanetExists(TARGET_PLANET_ID);

        verify(planetRepository, times(1)).existsById(TARGET_PLANET_ID);
    }
}
