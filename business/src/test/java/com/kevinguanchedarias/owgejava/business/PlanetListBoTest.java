package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.planet.PlanetCleanerService;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.PlanetListDto;
import com.kevinguanchedarias.owgejava.entity.PlanetList;
import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;
import com.kevinguanchedarias.owgejava.repository.PlanetListRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.business.PlanetListBo.PLANET_USER_LIST_CHANGE;
import static com.kevinguanchedarias.owgejava.mock.PlanetListMock.PLANET_LIST_NAME;
import static com.kevinguanchedarias.owgejava.mock.PlanetListMock.givenPlanetList;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = PlanetListBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetRepository.class,
        PlanetListRepository.class,
        SocketIoService.class,
        UserStorageBo.class,
        PlanetCleanerService.class,
        DtoUtilService.class
})
class PlanetListBoTest {
    private final PlanetListBo planetListBo;
    private final PlanetRepository planetRepository;
    private final PlanetListRepository repository;
    private final SocketIoService socketIoService;
    private final UserStorageBo userStorageBo;
    private final PlanetCleanerService planetCleanerService;
    private final DtoUtilService dtoUtilService;

    private final PlanetList planetList = givenPlanetList();
    private PlanetListDto planetListDtoMock;
    private PlanetDto planetDtoMock;
    private InvokeSupplierLambdaAnswer<List<PlanetListDto>> socketAnswer;

    @Autowired
    PlanetListBoTest(
            PlanetListBo planetListBo,
            PlanetRepository planetRepository,
            PlanetListRepository repository,
            SocketIoService socketIoService,
            UserStorageBo userStorageBo,
            PlanetCleanerService planetCleanerService,
            DtoUtilService dtoUtilService
    ) {
        this.planetListBo = planetListBo;
        this.planetRepository = planetRepository;
        this.repository = repository;
        this.socketIoService = socketIoService;
        this.userStorageBo = userStorageBo;
        this.planetCleanerService = planetCleanerService;
        this.dtoUtilService = dtoUtilService;
    }

    @BeforeEach
    void init_handle_emit_change_to_user() {
        planetListDtoMock = mock(PlanetListDto.class);
        planetDtoMock = mock(PlanetDto.class);
        given(repository.findByPlanetUserUserId(USER_ID_1)).willReturn(List.of(planetList));
        given(dtoUtilService.convertEntireArray(PlanetListDto.class, List.of(planetList))).willReturn(List.of(planetListDtoMock));
        given(planetListDtoMock.getPlanet()).willReturn(planetDtoMock);
        socketAnswer = new InvokeSupplierLambdaAnswer<>(2);
        doAnswer(socketAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(PLANET_USER_LIST_CHANGE), any());
    }

    @Test
    void findByUserId_should_work() {
        var retVal = planetListBo.findByUserId(USER_ID_1);

        assertThat(retVal).hasSize(1).containsExactly(planetListDtoMock);
        verify(planetCleanerService, times(1)).cleanUpUnexplored(USER_ID_1, planetDtoMock);
    }

    @Test
    void myAdd_should_work() {
        var user = givenUser1();
        var planet = givenTargetPlanet();
        given(userStorageBo.findLoggedInWithReference()).willReturn(user);
        given(planetRepository.findById(TARGET_PLANET_ID)).willReturn(Optional.of(planet));

        planetListBo.myAdd(TARGET_PLANET_ID, PLANET_LIST_NAME);

        var captor = ArgumentCaptor.forClass(PlanetList.class);
        verify(repository, times(1)).save(captor.capture());
        var savedValue = captor.getValue();
        var savedPlanetUser = savedValue.getPlanetUser();
        assertThat(savedPlanetUser.getPlanet()).isEqualTo(planet);
        assertThat(savedPlanetUser.getUser()).isEqualTo(user);
        verifySocketMessage();
    }

    @Test
    void myDelete_should_work() {
        var user = givenUser1();
        var planet = givenTargetPlanet();
        given(userStorageBo.findLoggedInWithReference()).willReturn(user);
        given(planetRepository.getById(TARGET_PLANET_ID)).willReturn(planet);

        planetListBo.myDelete(TARGET_PLANET_ID);

        var captor = ArgumentCaptor.forClass(PlanetUser.class);
        verify(repository, times(1)).deleteById(captor.capture());
        var deletedId = captor.getValue();
        assertThat(deletedId.getUser()).isEqualTo(user);
        assertThat(deletedId.getPlanet()).isEqualTo(planet);
        verifySocketMessage();
    }

    @Test
    void emitByChangedPlanet_should_work() {
        var planet = givenTargetPlanet();
        given(repository.findUserIdByPlanetListPlanet(planet)).willReturn(List.of(USER_ID_1));

        planetListBo.emitByChangedPlanet(planet);

        verifySocketMessage();
    }

    private void verifySocketMessage() {
        assertThat(socketAnswer.getResult()).hasSize(1).containsExactly(planetListDtoMock);
    }
}
