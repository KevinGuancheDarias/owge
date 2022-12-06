package com.kevinguanchedarias.owgejava.business.planet;

import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.SpecialLocationDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = PlanetCleanerService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(PlanetExplorationService.class)
class PlanetCleanerServiceTest {
    private static final String EXPECTED_STRING = "FOO_BAR";
    private static final int EXPECTED_NUMBER = 14;
    private static final boolean EXPECTED_BOOLEAN = false;
    private static final SpecialLocationDto EXPECTED_SPECIAL_LOCATION_DTO = new SpecialLocationDto();

    private final PlanetCleanerService planetCleanerService;
    private final PlanetExplorationService planetExplorationService;

    @Autowired
    PlanetCleanerServiceTest(PlanetCleanerService planetCleanerService, PlanetExplorationService planetExplorationService) {
        this.planetCleanerService = planetCleanerService;
        this.planetExplorationService = planetExplorationService;
    }

    @ParameterizedTest
    @MethodSource("cleanUpUnexplored_should_work_arguments")
    void cleanUpUnexplored_should_work(
            boolean isExplored,
            String expectedString,
            Boolean expectedBoolean,
            Integer expectedNumber,
            SpecialLocationDto expectedSpecialLocation
    ) {
        var planetDto = new PlanetDto();
        planetDto.setId(TARGET_PLANET_ID);
        planetDto.setName(EXPECTED_STRING);
        planetDto.setRichness(EXPECTED_NUMBER);
        planetDto.setHome(EXPECTED_BOOLEAN);
        planetDto.setOwnerId(EXPECTED_NUMBER);
        planetDto.setOwnerName(EXPECTED_STRING);
        planetDto.setSpecialLocation(EXPECTED_SPECIAL_LOCATION_DTO);
        given(planetExplorationService.isExplored(USER_ID_1, TARGET_PLANET_ID)).willReturn(isExplored);

        planetCleanerService.cleanUpUnexplored(USER_ID_1, planetDto);

        assertThat(planetDto.getId()).isEqualTo(TARGET_PLANET_ID);
        assertThat(planetDto.getName()).isEqualTo(expectedString);
        assertThat(planetDto.getRichness()).isEqualTo(expectedNumber);
        assertThat(planetDto.getHome()).isEqualTo(expectedBoolean);
        assertThat(planetDto.getOwnerId()).isEqualTo(expectedNumber);
        assertThat(planetDto.getOwnerName()).isEqualTo(expectedString);
        assertThat(planetDto.getSpecialLocation()).isEqualTo(expectedSpecialLocation);
    }

    private static Stream<Arguments> cleanUpUnexplored_should_work_arguments() {
        return Stream.of(
                Arguments.of(true, EXPECTED_STRING, EXPECTED_BOOLEAN, EXPECTED_NUMBER, EXPECTED_SPECIAL_LOCATION_DTO),
                Arguments.of(false, null, null, null, null)
        );
    }
}
