package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.SpecialLocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_ID;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.givenGalaxy;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenPlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.SpecialLocationMock.SPECIAL_LOCATION_ID;
import static com.kevinguanchedarias.owgejava.mock.SpecialLocationMock.givenSpecialLocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = SpecialLocationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        SpecialLocationRepository.class,
        PlanetRepository.class,
        PlanetBo.class
})
class SpecialLocationBoTest {
    private final SpecialLocationBo specialLocationBo;
    private final SpecialLocationRepository specialLocationRepository;
    private final PlanetRepository planetRepository;
    private final PlanetBo planetBo;

    @Autowired
    SpecialLocationBoTest(
            SpecialLocationBo specialLocationBo,
            SpecialLocationRepository specialLocationRepository,
            PlanetRepository planetRepository,
            PlanetBo planetBo
    ) {
        this.specialLocationBo = specialLocationBo;
        this.specialLocationRepository = specialLocationRepository;
        this.planetRepository = planetRepository;
        this.planetBo = planetBo;
    }

    @Test
    void save_should_handle_new_save() {
        var specialLocation = givenSpecialLocation();
        specialLocation.setId(null);
        given(specialLocationRepository.save(specialLocation)).will(returnsFirstArg());

        var retVal = specialLocationBo.save(specialLocation);

        verifyNoInteractions(planetRepository);
        assertThat(retVal).isSameAs(specialLocation);
    }

    @ParameterizedTest
    @MethodSource("save_should_handle_update_and_then_assign_planet_arguments")
    void save_should_handle_update_and_then_assign_planet(
            Galaxy newToSaveGalaxy,
            Galaxy storedAssignedGalaxy,
            Planet storedAssignedPlanet,
            Planet expectedAssignedPlanet,
            Galaxy expectedGalaxy,
            SpecialLocation expectedNewSpecialLocation,
            int timesSaveStoredPlanet,
            int timesSaveNewlyAssignedPlanet,
            int timesFindRandomPlanet
    ) {
        var specialLocation = givenSpecialLocation();
        specialLocation.setGalaxy(newToSaveGalaxy);
        specialLocation.setAssignedPlanet(givenPlanet(1234L));
        var storedSpecialLocation = givenSpecialLocation();
        storedSpecialLocation.setGalaxy(storedAssignedGalaxy);
        storedSpecialLocation.setAssignedPlanet(storedAssignedPlanet);
        var randomPlanet = givenTargetPlanet();
        given(specialLocationRepository.findById(SPECIAL_LOCATION_ID)).willReturn(Optional.of(storedSpecialLocation));
        given(planetBo.findRandomPlanet(GALAXY_ID)).willReturn(randomPlanet);
        given(specialLocationRepository.save(specialLocation)).will(returnsFirstArg());

        var retVal = specialLocationBo.save(specialLocation);

        assertThat(retVal).isSameAs(specialLocation);
        if (storedAssignedPlanet != null) {
            assertThat(storedAssignedPlanet.getSpecialLocation()).isNull();
            verify(planetRepository, times(timesSaveStoredPlanet)).save(storedAssignedPlanet);
        }
        assertThat(retVal.getGalaxy()).isEqualTo(expectedGalaxy);
        assertThat(retVal.getAssignedPlanet()).isEqualTo(expectedAssignedPlanet);
        assertThat(randomPlanet.getSpecialLocation()).isEqualTo(expectedNewSpecialLocation);
        verify(planetRepository, times(timesSaveNewlyAssignedPlanet)).save(randomPlanet);
        verify(planetBo, times(timesFindRandomPlanet)).findRandomPlanet(newToSaveGalaxy == null ? null : newToSaveGalaxy.getId());
    }

    @ParameterizedTest
    @MethodSource("delete_should_work_arguments")
    void delete_should_work(Planet assignedPlanet, int timesUpdateSpecialLocation) {
        var specialLocation = givenSpecialLocation();
        specialLocation.setAssignedPlanet(assignedPlanet);

        specialLocationBo.delete(specialLocation);

        verify(planetRepository, times(timesUpdateSpecialLocation)).updateSpecialLocation(anyLong(), eq(null));
        verify(specialLocationRepository, times(1)).delete(specialLocation);
    }

    private static Stream<Arguments> save_should_handle_update_and_then_assign_planet_arguments() {
        var galaxy = givenGalaxy();
        var expectedNewAssignedPlanet = givenTargetPlanet();
        var expectedNewSpecialLocation = givenSpecialLocation();
        expectedNewSpecialLocation.setGalaxy(galaxy);
        return Stream.of(
                Arguments.of(galaxy, null, givenPlanet(190L), expectedNewAssignedPlanet, galaxy, expectedNewSpecialLocation, 1, 1, 1),
                Arguments.of(galaxy, givenGalaxy(140), givenPlanet(180L), expectedNewAssignedPlanet, galaxy, expectedNewSpecialLocation, 1, 1, 1),
                Arguments.of(galaxy, galaxy, givenPlanet(180L), null, null, null, 0, 0, 0),
                Arguments.of(galaxy, null, null, expectedNewAssignedPlanet, galaxy, expectedNewSpecialLocation, 0, 1, 1),
                Arguments.of(givenGalaxy(0), null, null, null, null, null, 0, 0, 0),
                Arguments.of(null, null, null, null, null, null, 0, 0, 0)
        );
    }

    private static Stream<Arguments> delete_should_work_arguments() {
        return Stream.of(
                Arguments.of(givenTargetPlanet(), 1),
                Arguments.of(null, 0)
        );
    }
}
