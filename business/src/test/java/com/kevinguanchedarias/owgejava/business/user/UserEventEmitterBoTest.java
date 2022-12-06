package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementChangeEnum;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeBiConsumerLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo.USER_DATA_CHANGE;
import static com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo.USER_MAX_ENERGY_CHANGE;
import static com.kevinguanchedarias.owgejava.mock.AllianceMock.givenAlliance;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenFaction;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_ID;
import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.GALAXY_NAME;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = UserEventEmitterBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        SocketIoService.class,
        ImprovementBo.class,
        DtoUtilService.class,
        UserEnergyServiceBo.class,
        UserStorageRepository.class,
        TransactionUtilService.class
})
class UserEventEmitterBoTest {
    private final UserEventEmitterBo userEventEmitterBo;
    private final SocketIoService socketIoService;
    private final ImprovementBo improvementBo;
    private final DtoUtilService dtoUtilService;
    private final UserEnergyServiceBo userEnergyServiceBo;
    private final UserStorageRepository userStorageRepository;
    private final TransactionUtilService transactionUtilService;

    @Autowired
    UserEventEmitterBoTest(
            UserEventEmitterBo userEventEmitterBo,
            SocketIoService socketIoService,
            ImprovementBo improvementBo,
            DtoUtilService dtoUtilService,
            UserEnergyServiceBo userEnergyServiceBo,
            UserStorageRepository userStorageRepository,
            TransactionUtilService transactionUtilService
    ) {
        this.userEventEmitterBo = userEventEmitterBo;
        this.socketIoService = socketIoService;
        this.improvementBo = improvementBo;
        this.dtoUtilService = dtoUtilService;
        this.userEnergyServiceBo = userEnergyServiceBo;
        this.userStorageRepository = userStorageRepository;
        this.transactionUtilService = transactionUtilService;
    }

    @Test
    void init_should_work() {
        var answerUnitImprovementsChangeConsumer = new InvokeBiConsumerLambdaAnswer<Integer, Improvement>(1);
        doAnswer(answerUnitImprovementsChangeConsumer).when(improvementBo).addChangeListener(eq(ImprovementChangeEnum.MORE_ENERGY), any());
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        var user = givenUser1();
        double maxEnergy = 9;
        given(userStorageRepository.getById(USER_ID_1)).willReturn(user);
        var socketAnswerSupplier = new InvokeSupplierLambdaAnswer<Double>(2);
        doAnswer(socketAnswerSupplier).when(socketIoService).sendMessage(eq(USER_ID_1), eq(USER_MAX_ENERGY_CHANGE), any());
        given(userEnergyServiceBo.findMaxEnergy(user)).willReturn(maxEnergy);

        userEventEmitterBo.emitMaxEnergyChange(USER_ID_1);

        assertThat(socketAnswerSupplier.getResult()).isEqualTo(maxEnergy);
    }

    @Test
    void emitMaxEnergyChange_should_work() {
        var user = givenUser1();
        double maxEnergy = 9;
        given(userStorageRepository.getById(USER_ID_1)).willReturn(user);
        var socketAnswerSupplier = new InvokeSupplierLambdaAnswer<Double>(2);
        doAnswer(socketAnswerSupplier).when(socketIoService).sendMessage(eq(USER_ID_1), eq(USER_MAX_ENERGY_CHANGE), any());
        given(userEnergyServiceBo.findMaxEnergy(user)).willReturn(maxEnergy);

        userEventEmitterBo.emitMaxEnergyChange(USER_ID_1);

        assertThat(socketAnswerSupplier.getResult()).isEqualTo(maxEnergy);
    }

    @Test
    void emitUserData_should_work() {
        var user = givenUser1().toBuilder()
                .faction(givenFaction())
                .homePlanet(givenSourcePlanet())
                .alliance(givenAlliance())
                .build();
        var socketAnswerSupplier = new InvokeSupplierLambdaAnswer<UserStorageDto>(2);
        var groupedImprovementMock = mock(GroupedImprovement.class);
        var factionMock = mock(FactionDto.class);
        var homePlanetMock = mock(PlanetDto.class);
        var allianceMock = mock(AllianceDto.class);
        double consumedEnergy = 190;
        double maxEnergy = 220;

        doAnswer(socketAnswerSupplier).when(socketIoService).sendMessage(eq(user), eq(USER_DATA_CHANGE), any());
        given(improvementBo.findUserImprovement(user)).willReturn(groupedImprovementMock);
        given(dtoUtilService.dtoFromEntity(FactionDto.class, user.getFaction())).willReturn(factionMock);
        given(dtoUtilService.dtoFromEntity(PlanetDto.class, user.getHomePlanet())).willReturn(homePlanetMock);
        given(dtoUtilService.dtoFromEntity(AllianceDto.class, user.getAlliance())).willReturn(allianceMock);
        given(userEnergyServiceBo.findConsumedEnergy(user)).willReturn(consumedEnergy);
        given(userEnergyServiceBo.findMaxEnergy(user)).willReturn(maxEnergy);

        userEventEmitterBo.emitUserData(user);

        var sentUser = socketAnswerSupplier.getResult();
        assertThat(sentUser.getId()).isEqualTo(USER_ID_1);
        assertThat(sentUser.getImprovements()).isEqualTo(groupedImprovementMock);
        assertThat(sentUser.getFactionDto()).isEqualTo(factionMock);
        assertThat(sentUser.getHomePlanetDto()).isEqualTo(homePlanetMock);
        assertThat(sentUser.getAlliance()).isEqualTo(allianceMock);
        verify(homePlanetMock, times(1)).setGalaxyId(GALAXY_ID);
        verify(homePlanetMock, times(1)).setGalaxyName(GALAXY_NAME);
        assertThat(sentUser.getConsumedEnergy()).isEqualTo(consumedEnergy);
        assertThat(sentUser.getMaxEnergy()).isEqualTo(maxEnergy);

    }
}
