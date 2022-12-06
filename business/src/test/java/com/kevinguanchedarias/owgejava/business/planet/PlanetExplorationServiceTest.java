package com.kevinguanchedarias.owgejava.business.planet;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.entity.ExploredPlanet;
import com.kevinguanchedarias.owgejava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService.PLANET_EXPLORED_EVENT;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = PlanetExplorationService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetRepository.class,
        ExploredPlanetRepository.class,
        SocketIoService.class,
        DtoUtilService.class
})
class PlanetExplorationServiceTest {
    private final PlanetExplorationService planetExplorationService;
    private final PlanetRepository planetRepository;
    private final ExploredPlanetRepository exploredPlanetRepository;
    private final SocketIoService socketIoService;
    private final DtoUtilService dtoUtilService;

    @Autowired
    PlanetExplorationServiceTest(
            PlanetExplorationService planetExplorationService,
            PlanetRepository planetRepository,
            ExploredPlanetRepository exploredPlanetRepository,
            SocketIoService socketIoService,
            DtoUtilService dtoUtilService
    ) {
        this.planetExplorationService = planetExplorationService;
        this.planetRepository = planetRepository;
        this.exploredPlanetRepository = exploredPlanetRepository;
        this.socketIoService = socketIoService;
        this.dtoUtilService = dtoUtilService;
    }

    @ParameterizedTest
    @MethodSource("isExplored_should_work_arguments")
    void isExplored_should_work(boolean isOfUserProperty, ExploredPlanet exploredPlanet, boolean expectation) {
        var user = givenUser1();
        var planet = givenSourcePlanet();
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(isOfUserProperty);
        given(exploredPlanetRepository.findOneByUserIdAndPlanetId(USER_ID_1, SOURCE_PLANET_ID)).willReturn(exploredPlanet);

        var retVal = planetExplorationService.isExplored(user, planet);

        assertThat(retVal).isEqualTo(expectation);
    }

    private static Stream<Arguments> isExplored_should_work_arguments() {
        return Stream.of(
                Arguments.of(true, null, true),
                Arguments.of(false, null, false),
                Arguments.of(false, ExploredPlanet.builder().build(), true)
        );
    }

    @Test
    void defineAsExplored_should_work() {
        var user = givenUser1();
        var planet = givenTargetPlanet();
        var expectedSocketContent = mock(PlanetDto.class);
        var socketAnswer = new InvokeSupplierLambdaAnswer<PlanetDto>(2);
        doAnswer(socketAnswer).when(socketIoService).sendMessage(eq(user), eq(PLANET_EXPLORED_EVENT), any());
        given(planetRepository.findById(TARGET_PLANET_ID)).willReturn(Optional.of(planet));
        given(dtoUtilService.dtoFromEntity(PlanetDto.class, planet)).willReturn(expectedSocketContent);

        planetExplorationService.defineAsExplored(user, planet);

        var captor = ArgumentCaptor.forClass(ExploredPlanet.class);
        verify(exploredPlanetRepository, times(1)).save(captor.capture());
        var savedExplored = captor.getValue();
        assertThat(savedExplored.getUser()).isEqualTo(user);
        assertThat(savedExplored.getPlanet()).isEqualTo(planet);
        assertThat(socketAnswer.getResult()).isSameAs(expectedSocketContent);

    }
}
