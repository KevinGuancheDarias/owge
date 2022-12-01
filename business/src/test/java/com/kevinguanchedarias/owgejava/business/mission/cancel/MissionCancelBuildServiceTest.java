package com.kevinguanchedarias.owgejava.business.mission.cancel;


import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitModificationBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.business.MissionBo.UNIT_BUILD_MISSION_CHANGE;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MissionCancelBuildService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UserStorageRepository.class,
        ObtainedUnitModificationBo.class,
        TransactionUtilService.class,
        SocketIoService.class,
        UnitTypeBo.class,
        MissionEventEmitterBo.class,
        MissionFinderBo.class
})
class MissionCancelBuildServiceTest {
    private final MissionCancelBuildService missionCancelBuildService;
    private final UserStorageRepository userStorageRepository;
    private final ObtainedUnitModificationBo obtainedUnitModificationBo;
    private final TransactionUtilService transactionUtilService;
    private final SocketIoService socketIoService;
    private final UnitTypeBo unitTypeBo;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MissionFinderBo missionFinderBo;

    @Autowired
    MissionCancelBuildServiceTest(
            MissionCancelBuildService missionCancelBuildService,
            UserStorageRepository userStorageRepository,
            ObtainedUnitModificationBo obtainedUnitModificationBo,
            TransactionUtilService transactionUtilService,
            SocketIoService socketIoService,
            UnitTypeBo unitTypeBo,
            MissionEventEmitterBo missionEventEmitterBo,
            MissionFinderBo missionFinderBo
    ) {
        this.missionCancelBuildService = missionCancelBuildService;
        this.userStorageRepository = userStorageRepository;
        this.obtainedUnitModificationBo = obtainedUnitModificationBo;
        this.transactionUtilService = transactionUtilService;
        this.socketIoService = socketIoService;
        this.unitTypeBo = unitTypeBo;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.missionFinderBo = missionFinderBo;
    }

    @Test
    void cancel_should_work() {
        var mission = givenExploreMission();
        double primary = 190;
        double secondary = 220;
        mission.setPrimaryResource(primary);
        mission.setSecondaryResource(secondary);
        var userSpy = spy(givenUser1());
        var expectedBuildMissions = List.of(mock(RunningUnitBuildDto.class));
        mission.setUser(userSpy);
        doNothing().when(userSpy).addtoPrimary(anyDouble());
        doNothing().when(userSpy).addToSecondary(anyDouble());
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        var socketMessageSupplier = new InvokeSupplierLambdaAnswer<List<RunningUnitBuildDto>>(2);
        doAnswer(socketMessageSupplier).when(socketIoService).sendMessage(eq(userSpy), eq(UNIT_BUILD_MISSION_CHANGE), any());
        given(missionFinderBo.findBuildMissions(USER_ID_1)).willReturn(expectedBuildMissions);

        missionCancelBuildService.cancel(mission);

        verify(obtainedUnitModificationBo, times(1)).deleteByMissionId(EXPLORE_MISSION_ID);
        verify(userSpy, times(1)).addtoPrimary(primary);
        verify(userSpy, times(1)).addToSecondary(secondary);
        verify(userStorageRepository, times(1)).save(userSpy);
        assertThat(socketMessageSupplier.getResult()).isSameAs(expectedBuildMissions);
        verify(unitTypeBo, times(1)).emitUserChange(USER_ID_1);
        verify(missionEventEmitterBo, times(1)).emitMissionCountChange(USER_ID_1);
    }
}
