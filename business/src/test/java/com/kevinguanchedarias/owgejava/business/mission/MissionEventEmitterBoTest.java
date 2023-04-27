package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.business.MissionBo.UNIT_BUILD_MISSION_CHANGE;
import static com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo.*;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenGatherMission;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MissionEventEmitterBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        TransactionUtilService.class,
        EntityManager.class,
        SocketIoService.class,
        RunningMissionFinderBo.class,
        MissionRepository.class,
        MissionFinderBo.class
})
class MissionEventEmitterBoTest {
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final TransactionUtilService transactionUtilService;
    private final EntityManager entityManager;
    private final SocketIoService socketIoService;
    private final RunningMissionFinderBo runningMissionFinderBo;
    private final MissionRepository missionRepository;
    private final MissionFinderBo missionFinderBo;

    @Autowired
    public MissionEventEmitterBoTest(
            MissionEventEmitterBo missionEventEmitterBo,
            TransactionUtilService transactionUtilService,
            EntityManager entityManager,
            SocketIoService socketIoService,
            RunningMissionFinderBo runningMissionFinderBo,
            MissionRepository missionRepository,
            MissionFinderBo missionFinderBo
    ) {
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.transactionUtilService = transactionUtilService;
        this.entityManager = entityManager;
        this.socketIoService = socketIoService;
        this.runningMissionFinderBo = runningMissionFinderBo;
        this.missionRepository = missionRepository;
        this.missionFinderBo = missionFinderBo;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void emitLocalMissionChangeAfterCommit_should_work(boolean isInvisible) {
        var mission = givenExploreMission();
        mission.setInvisible(isInvisible);
        var enemyMission = givenGatherMission();
        var user = givenUser1();
        var planetOwner = givenUser2();
        enemyMission.setUser(planetOwner);
        mission.setUser(user);
        mission.getTargetPlanet().setOwner(planetOwner);
        var ownerMissionsAnswer = new InvokeSupplierLambdaAnswer<List<UnitRunningMissionDto>>(2);
        var userMissionsAnswer = new InvokeSupplierLambdaAnswer<MissionWebsocketMessage>(2);
        var userMissions = List.of(new UnitRunningMissionDto(mission));
        var enemyMissions = List.of(new UnitRunningMissionDto(enemyMission));
        var count = 1;
        var expectedUserMissionsObject = MissionWebsocketMessage.builder()
                .myUnitMissions(userMissions)
                .count(count)
                .build();
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(Mockito.any());
        doAnswer(ownerMissionsAnswer).when(socketIoService).sendMessage(eq(planetOwner), eq(ENEMY_MISSION_CHANGE), any());
        doAnswer(userMissionsAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(UNIT_MISSION_CHANGE), any());
        given(runningMissionFinderBo.findEnemyRunningMissions(planetOwner)).willReturn(enemyMissions);
        given(runningMissionFinderBo.findUserRunningMissions(USER_ID_1)).willReturn(userMissions);
        given(runningMissionFinderBo.countUserRunningMissions(USER_ID_1)).willReturn(count);

        missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission);

        verify(entityManager, times(1)).refresh(mission);
        assertThat(ownerMissionsAnswer.getResult()).isEqualTo(isInvisible ? null : enemyMissions);
        assertThat(userMissionsAnswer.getResult()).isEqualTo(expectedUserMissionsObject);
    }

    @ParameterizedTest
    @MethodSource("emitEnemyMissionsChange_should_work_arguments")
    void emitEnemyMissionsChange_should_work(
            UserStorage planetOwner, int times
    ) {
        var mission = givenExploreMission();
        mission.setUser(givenUser1());
        mission.getTargetPlanet().setOwner(planetOwner);
        doAnswer(new InvokeSupplierLambdaAnswer<>(2)).when(socketIoService).sendMessage(eq(planetOwner), eq(ENEMY_MISSION_CHANGE), any());

        missionEventEmitterBo.emitEnemyMissionsChange(List.of(mission));

        verify(socketIoService, times(times)).sendMessage(eq(planetOwner), eq(ENEMY_MISSION_CHANGE), any());
        verify(runningMissionFinderBo, times(times)).findEnemyRunningMissions(planetOwner);
    }

    @Test
    void emitMissionCountChange_should_work() {
        int count = 9;
        given(missionRepository.countByUserIdAndResolvedFalse(USER_ID_1)).willReturn(count);
        var countChangeAnswer = new InvokeSupplierLambdaAnswer<Integer>(2);
        doAnswer(countChangeAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(MISSIONS_COUNT_CHANGE), any());

        missionEventEmitterBo.emitMissionCountChange(USER_ID_1);

        var sentMessage = countChangeAnswer.getResult();
        assertThat(sentMessage).isEqualTo(count);
    }

    @Test
    void emitUnitBuildChange_should_work() {
        var runningUnitBuild = mock(RunningUnitBuildDto.class);
        given(missionFinderBo.findBuildMissions(USER_ID_1)).willReturn(List.of(runningUnitBuild));
        var countChangeAnswer = new InvokeSupplierLambdaAnswer<List<RunningUnitBuildDto>>(2);
        doAnswer(countChangeAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(UNIT_BUILD_MISSION_CHANGE), any());

        missionEventEmitterBo.emitUnitBuildChange(USER_ID_1);

        var sentMessage = countChangeAnswer.getResult();
        assertThat(sentMessage).hasSize(1).contains(runningUnitBuild);
    }

    @Test
    void emitUnitMissionsAfterCommit_should_work() {
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());

        missionEventEmitterBo.emitUnitMissionsAfterCommit(USER_ID_1);

        verify(transactionUtilService, times(1)).doAfterCommit(any());
        verify(socketIoService, times(1)).sendMessage(eq(USER_ID_1), eq(UNIT_MISSION_CHANGE), any());
    }

    private static Stream<Arguments> emitEnemyMissionsChange_should_work_arguments() {
        return Stream.of(
                Arguments.of(givenUser1(), 0),
                Arguments.of(givenUser2(), 1),
                Arguments.of(null, 0)
        );
    }
}
