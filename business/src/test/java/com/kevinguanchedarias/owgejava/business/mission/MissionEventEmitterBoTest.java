package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.util.List;

import static com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo.ENEMY_MISSION_CHANGE;
import static com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo.UNIT_MISSION_CHANGE;
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
        RunningMissionFinderBo.class
})
class MissionEventEmitterBoTest {
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final TransactionUtilService transactionUtilService;
    private final EntityManager entityManager;
    private final SocketIoService socketIoService;
    private final RunningMissionFinderBo runningMissionFinderBo;

    @Autowired
    public MissionEventEmitterBoTest(
            MissionEventEmitterBo missionEventEmitterBo,
            TransactionUtilService transactionUtilService,
            EntityManager entityManager,
            SocketIoService socketIoService,
            RunningMissionFinderBo runningMissionFinderBo
    ) {
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.transactionUtilService = transactionUtilService;
        this.entityManager = entityManager;
        this.socketIoService = socketIoService;
        this.runningMissionFinderBo = runningMissionFinderBo;
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
}
