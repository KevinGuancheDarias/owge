package com.kevinguanchedarias.owgejava.business.schedule;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.ScheduledTasksManagerService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeConsumerLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.kevinguanchedarias.owgejava.business.schedule.TemporalUnitScheduleListener.TASK_NAME;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = TemporalUnitScheduleListener.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUnitRepository.class,
        ObtainedUnitBo.class,
        ScheduledTasksManagerService.class,
        PlanetLockUtilService.class,
        TransactionUtilService.class,
        ObtainedUnitTemporalInformationRepository.class,
        MissionRepository.class,
        MissionBo.class
})
class TemporalUnitScheduleListenerTest {
    private final TemporalUnitScheduleListener temporalUnitScheduleListener;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ScheduledTasksManagerService scheduledTasksManagerService;
    private final PlanetLockUtilService planetLockUtilService;
    private final TransactionUtilService transactionUtilService;
    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;
    private final MissionRepository missionRepository;
    private final MissionBo missionBo;

    @Autowired
    public TemporalUnitScheduleListenerTest(
            TemporalUnitScheduleListener temporalUnitScheduleListener,
            ObtainedUnitRepository obtainedUnitRepository,
            ObtainedUnitBo obtainedUnitBo,
            ScheduledTasksManagerService scheduledTasksManagerService,
            PlanetLockUtilService planetLockUtilService,
            TransactionUtilService transactionUtilService,
            ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository,
            MissionRepository missionRepository,
            MissionBo missionBo
    ) {
        this.temporalUnitScheduleListener = temporalUnitScheduleListener;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.obtainedUnitBo = obtainedUnitBo;
        this.scheduledTasksManagerService = scheduledTasksManagerService;
        this.planetLockUtilService = planetLockUtilService;
        this.transactionUtilService = transactionUtilService;
        this.obtainedUnitTemporalInformationRepository = obtainedUnitTemporalInformationRepository;
        this.missionRepository = missionRepository;
        this.missionBo = missionBo;
    }

    @ParameterizedTest
    @CsvSource({
            "true,false,false,false,false",
            "false,false,false,false,false",
            "false,true,false,false,false",
            "false,true,true,false,false",
            "false,true,true,true,false",
            "false,true,true,true,true"
    })
    void init_should_register_handle_and_that_handler_should_work(
            boolean isEmptyList,
            boolean hasAffectedMissions,
            boolean affectedMissionHasOwner,
            boolean affectedMissionHasUnit,
            boolean planetOwnerIsUser
    ) {
        var invokeHandlerAnswer = new InvokeConsumerLambdaAnswer<ScheduledTask>(1);
        var expirationId = 8L;
        var invocations = new AtomicInteger(0);
        var planetsForLocks = List.of(
                Set.of(14, 12),
                Set.of(18, 11),
                Set.of(18, 11),
                Set.of(18, 11)
        );
        doAnswer(invokeHandlerAnswer).when(scheduledTasksManagerService).addHandler(eq(TASK_NAME), any());
        doAnswer(invocationOnMock -> planetsForLocks.get(invocations.getAndIncrement()))
                .when(obtainedUnitRepository).findPlanetIdsByExpirationId(expirationId);
        doAnswer(new InvokeRunnableLambdaAnswer(1)).when(planetLockUtilService).doInsideLockById(any(), any());
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).runWithRequired(any());
        var ou = givenObtainedUnit1();
        var affectedMission = givenExploreMission();
        var user = ou.getUser();
        affectedMission.setUser(user);
        if (hasAffectedMissions) {
            ou.setMission(affectedMission);
        }
        var user2 = givenUser2();
        if (affectedMissionHasOwner) {
            affectedMission.getTargetPlanet().setOwner(user2);
            given(obtainedUnitRepository.existsByMission(affectedMission)).willReturn(affectedMissionHasUnit);
        }
        if (planetOwnerIsUser) {
            affectedMission.getTargetPlanet().setOwner(user);
        }
        given(obtainedUnitRepository.findByExpirationId(expirationId)).willReturn(isEmptyList ? List.of() : List.of(ou));

        var task = ScheduledTask.builder().content((double) expirationId).build();

        temporalUnitScheduleListener.init();
        invokeHandlerAnswer.getPassedLambda().accept(task);

        verify(obtainedUnitRepository, times(4)).findPlanetIdsByExpirationId(expirationId);
        verify(obtainedUnitBo, times(isEmptyList ? 0 : 1)).delete(ou);
        verify(obtainedUnitBo, times(isEmptyList ? 0 : 1)).emitObtainedUnitChange(USER_ID_1);
        verify(obtainedUnitTemporalInformationRepository, times(1)).deleteById(expirationId);
        verify(obtainedUnitRepository, times(!isEmptyList && hasAffectedMissions ? 1 : 0)).existsByMission(affectedMission);
        verify(missionRepository, times(!isEmptyList && hasAffectedMissions && !affectedMissionHasUnit ? 1 : 0))
                .delete(affectedMission);
        verify(missionBo, times(!isEmptyList && hasAffectedMissions ? 1 : 0)).emitUnitMissions(USER_ID_1);
        verify(missionBo, times(!isEmptyList && hasAffectedMissions ? 1 : 0)).emitMissionCountChange(USER_ID_1);
        verify(missionBo, times(!isEmptyList && hasAffectedMissions && affectedMissionHasOwner && !planetOwnerIsUser ? 1 : 0)).emitEnemyMissionsChange(user2);
    }
}
