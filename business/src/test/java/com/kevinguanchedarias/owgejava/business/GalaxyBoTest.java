package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_ID;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.givenGalaxy;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = GalaxyBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        GalaxyRepository.class,
        PlanetRepository.class
})
class GalaxyBoTest {
    private final GalaxyBo galaxyBo;
    private final GalaxyRepository galaxyRepository;
    private final PlanetRepository planetRepository;

    @Autowired
    GalaxyBoTest(GalaxyBo galaxyBo, GalaxyRepository galaxyRepository, PlanetRepository planetRepository) {
        this.galaxyBo = galaxyBo;
        this.galaxyRepository = galaxyRepository;
        this.planetRepository = planetRepository;
    }

    @ParameterizedTest
    @CsvSource({
            "0,2,2",
            "2,0,2",
            "2,2,0"
    })
    void save_should_throw_when_invalid_input(long sectors, long quadrants, long numPlanets) {
        var galaxy = givenGalaxy(sectors, quadrants, numPlanets);

        assertThatThrownBy(() -> galaxyBo.save(galaxy))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessage("Invalid input");
    }

    @Test
    void save_should_throw_when_galaxy_is_too_big() {
        var galaxy = givenGalaxy(100, 100, 100);

        assertThatThrownBy(() -> galaxyBo.save(galaxy))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("Galaxy can't have more than");
    }

    @Test
    void save_should_throw_when_galaxy_has_players() {
        var galaxy = givenGalaxy();
        given(planetRepository.findOneByGalaxyIdAndOwnerNotNullOrderByGalaxyId(GALAXY_ID)).willReturn(givenSourcePlanet());

        assertThatThrownBy(() -> galaxyBo.save(galaxy))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("already has players");
    }

    @ParameterizedTest
    @MethodSource("save_should_work_arguments")
    void save_should_work(Integer galaxyId, int timesDeletePlanets, List<Planet> mutableInitialPlanets) {
        var galaxy = givenGalaxy().toBuilder().id(galaxyId).planets(mutableInitialPlanets).build();
        given(galaxyRepository.saveAndFlush(galaxy)).willReturn(galaxy);


        var retVal = galaxyBo.save(galaxy);

        verify(planetRepository, times(timesDeletePlanets)).deleteByGalaxyId(galaxyId);
        var addedPlanets = retVal.getPlanets();
        assertThat(addedPlanets).hasSize(500);
        var firstPlanet = addedPlanets.get(0);
        assertThat(firstPlanet.getName()).isEqualTo("VS1C1N1");
        assertThat(firstPlanet.getRichness()).isNotNull();
        assertThat(firstPlanet.getGalaxy()).isSameAs(galaxy);
        assertThat(firstPlanet.getSector()).isEqualTo(1);
        assertThat(firstPlanet.getQuadrant()).isEqualTo(1);
        assertThat(firstPlanet.getPlanetNumber()).isEqualTo(1);
        var lastPlanet = addedPlanets.get(499);
        assertThat(lastPlanet.getName()).isEqualTo("VS5C5N20");
        assertThat(lastPlanet.getRichness()).isNotNull();
        assertThat(lastPlanet.getGalaxy()).isSameAs(galaxy);
        assertThat(lastPlanet.getSector()).isEqualTo(5);
        assertThat(lastPlanet.getQuadrant()).isEqualTo(5);
        assertThat(lastPlanet.getPlanetNumber()).isEqualTo(20);
        verify(galaxyRepository, times(1)).saveAndFlush(galaxy);
    }

    @Test
    void coordinatesToString_should_work() {
        assertThat(galaxyBo.coordinatesToString(1, 2, 3)).isEqualTo("1-2-3");
    }

    private static Stream<Arguments> save_should_work_arguments() {
        return Stream.of(
                Arguments.of(null, 0, null),
                Arguments.of(GALAXY_ID, 1, null),
                Arguments.of(GALAXY_ID, 1, new ArrayList<>())
        );
    }
}
