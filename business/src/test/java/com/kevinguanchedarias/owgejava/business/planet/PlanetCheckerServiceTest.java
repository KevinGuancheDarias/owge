package com.kevinguanchedarias.owgejava.business.planet;

import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = PlanetCheckerService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetRepository.class,
        UserStorageBo.class
})
class PlanetCheckerServiceTest {
    private final PlanetCheckerService planetCheckerService;
    private final PlanetRepository planetRepository;
    private final UserStorageBo userStorageBo;

    @Autowired
    PlanetCheckerServiceTest(PlanetCheckerService planetCheckerService, PlanetRepository planetRepository, UserStorageBo userStorageBo) {
        this.planetCheckerService = planetCheckerService;
        this.planetRepository = planetRepository;
        this.userStorageBo = userStorageBo;
    }

    @Test
    void myCheckIsOfUserProperty_should_throw() {
        var user = givenUser1();
        given(userStorageBo.findLoggedIn()).willReturn(user);

        assertThatThrownBy(() -> planetCheckerService.myCheckIsOfUserProperty(TARGET_PLANET_ID))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("does NOT belong to the user");
    }

    @Test
    void myCheckIsOfUserProperty_works() {
        var user = givenUser1();
        given(userStorageBo.findLoggedIn()).willReturn(user);
        given(planetRepository.isOfUserProperty(USER_ID_1, TARGET_PLANET_ID)).willReturn(true);

        planetCheckerService.myCheckIsOfUserProperty(TARGET_PLANET_ID);

        verify(planetRepository, times(1)).isOfUserProperty(USER_ID_1, TARGET_PLANET_ID);
    }
}
