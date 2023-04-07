package com.kevinguanchedarias.owgejava.business.schedule;

import com.kevinguanchedarias.owgejava.business.ScheduledTasksManagerService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.business.util.UnitImprovementUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeConsumerLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveTemporalUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID;
import static com.kevinguanchedarias.owgejava.business.schedule.TemporalUnitScheduleListener.TASK_NAME;
import static com.kevinguanchedarias.owgejava.mock.ActiveTimeSpecialMock.givenActiveTimeSpecialMock;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.*;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitTemporalInformationMock.OBTAINED_UNIT_TEMPORAL_INFORMATION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitTemporalInformationMock.givenObtainedUnitTemporalInformation;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.SOURCE_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleDto;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.givenTimeSpecial;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = TemporalUnitScheduleListener.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUnitRepository.class,
        ScheduledTasksManagerService.class,
        PlanetLockUtilService.class,
        TransactionUtilService.class,
        ObtainedUnitTemporalInformationRepository.class,
        MissionRepository.class,
        ObtainedUnitEventEmitter.class,
        MissionEventEmitterBo.class,
        TaggableCacheManager.class,
        UnitImprovementUtilService.class,
        ActiveTimeSpecialRepository.class,
        RuleBo.class,
        ObjectRelationsRepository.class
})
class TemporalUnitScheduleListenerTest {
    private final TemporalUnitScheduleListener temporalUnitScheduleListener;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ScheduledTasksManagerService scheduledTasksManagerService;
    private final PlanetLockUtilService planetLockUtilService;
    private final TransactionUtilService transactionUtilService;
    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;
    private final MissionRepository missionRepository;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final TaggableCacheManager taggableCacheManager;
    private final UnitImprovementUtilService unitImprovementUtilService;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final RuleBo ruleBo;
    private final ObjectRelationsRepository objectRelationsRepository;

    @Autowired
    public TemporalUnitScheduleListenerTest(
            TemporalUnitScheduleListener temporalUnitScheduleListener,
            ObtainedUnitRepository obtainedUnitRepository,
            ScheduledTasksManagerService scheduledTasksManagerService,
            PlanetLockUtilService planetLockUtilService,
            TransactionUtilService transactionUtilService,
            ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository,
            MissionRepository missionRepository,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            MissionEventEmitterBo missionEventEmitterBo,
            TaggableCacheManager taggableCacheManager,
            UnitImprovementUtilService unitImprovementUtilService,
            ActiveTimeSpecialRepository activeTimeSpecialRepository,
            RuleBo ruleBo,
            ObjectRelationsRepository objectRelationsRepository
    ) {
        this.temporalUnitScheduleListener = temporalUnitScheduleListener;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.scheduledTasksManagerService = scheduledTasksManagerService;
        this.planetLockUtilService = planetLockUtilService;
        this.transactionUtilService = transactionUtilService;
        this.obtainedUnitTemporalInformationRepository = obtainedUnitTemporalInformationRepository;
        this.missionRepository = missionRepository;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.taggableCacheManager = taggableCacheManager;
        this.unitImprovementUtilService = unitImprovementUtilService;
        this.activeTimeSpecialRepository = activeTimeSpecialRepository;
        this.ruleBo = ruleBo;
        this.objectRelationsRepository = objectRelationsRepository;
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
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
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
        given(obtainedUnitTemporalInformationRepository.existsById(expirationId)).willReturn(true);
        given(obtainedUnitRepository.findByExpirationId(expirationId)).willReturn(isEmptyList ? List.of() : List.of(ou));

        var task = ScheduledTask.builder().content((double) expirationId).build();

        temporalUnitScheduleListener.init();
        invokeHandlerAnswer.getPassedLambda().accept(task);

        verify(obtainedUnitRepository, times(4)).findPlanetIdsByExpirationId(expirationId);
        verify(obtainedUnitRepository, times(isEmptyList ? 0 : 1)).deleteAll(List.of(ou));
        verify(unitImprovementUtilService, times(isEmptyList ? 0 : 1)).maybeTriggerClearImprovement(user, List.of(ou));
        verify(obtainedUnitEventEmitter, times(isEmptyList ? 0 : 1)).emitObtainedUnitsAfterCommit(user);
        verify(obtainedUnitTemporalInformationRepository, times(1)).deleteById(expirationId);
        verify(obtainedUnitRepository, times(!isEmptyList && hasAffectedMissions ? 1 : 0)).existsByMission(affectedMission);
        verify(missionRepository, times(!isEmptyList && hasAffectedMissions && !affectedMissionHasUnit ? 1 : 0))
                .delete(affectedMission);
        verify(missionEventEmitterBo, times(!isEmptyList && hasAffectedMissions ? 1 : 0)).emitUnitMissions(USER_ID_1);
        verify(missionEventEmitterBo, times(!isEmptyList && hasAffectedMissions ? 1 : 0)).emitMissionCountChange(USER_ID_1);
        verify(missionEventEmitterBo, times(!isEmptyList && hasAffectedMissions && affectedMissionHasOwner && !planetOwnerIsUser ? 1 : 0))
                .emitEnemyMissionsChange(user2);
        verify(taggableCacheManager, times(!isEmptyList && hasAffectedMissions ? 1 : 0)).evictByCacheTag(Mission.MISSION_BY_USER_CACHE_TAG, USER_ID_1);
    }

    @Test
    void handler_should_do_nothing_if_expiration_does_not_exists() {
        var expirationId = 123678;
        var invokeHandlerAnswer = new InvokeConsumerLambdaAnswer<ScheduledTask>(1);
        doAnswer(invokeHandlerAnswer).when(scheduledTasksManagerService).addHandler(eq(TASK_NAME), any());
        var task = ScheduledTask.builder().content((double) expirationId).build();

        temporalUnitScheduleListener.init();
        invokeHandlerAnswer.getPassedLambda().accept(task);

        verifyNoInteractions(planetLockUtilService, obtainedUnitRepository);
    }

    @Test
    void relationLost_should_do_nothing_if_is_not_a_time_special() {
        var ur = givenUnlockedRelation();

        temporalUnitScheduleListener.relationObtained(ur);

        verifyNoInteractions(activeTimeSpecialRepository);
    }


    @Test
    void relationLost_should_do_nothing_if_special_is_not_active() {
        var user = givenUser1();
        var ur = givenUnlockedRelation(user);
        ur.getRelation().setObject(givenObjectEntity(ObjectEnum.TIME_SPECIAL));
        ur.getRelation().setReferenceId(TIME_SPECIAL_ID);
        var ts = givenTimeSpecial();
        var ats = givenActiveTimeSpecialMock(TimeSpecialStateEnum.RECHARGE);
        given(activeTimeSpecialRepository.findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1)).willReturn(Optional.of(ats));

        temporalUnitScheduleListener.relationLost(ur);

        verify(activeTimeSpecialRepository, times(1)).findOneByTimeSpecialIdAndUserId(ts.getId(), USER_ID_1);
        verifyNoInteractions(ruleBo, objectRelationsRepository, obtainedUnitRepository, obtainedUnitTemporalInformationRepository);
    }

    @ParameterizedTest
    @MethodSource("relationLost_should_delete_units_affected_by_time_special_lost_arguments")
    void relationLost_should_delete_units_affected_by_time_special_lost(Set<Long> planetIds, int timesInteraction) {
        var user = givenUser1();
        var ur = givenUnlockedRelation(user);
        var or = ur.getRelation();
        ObjectEnum timeSpecialObject = ObjectEnum.TIME_SPECIAL;
        or.setObject(givenObjectEntity(timeSpecialObject));
        or.setReferenceId(TIME_SPECIAL_ID);
        var ats = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        given(activeTimeSpecialRepository.findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1)).willReturn(Optional.of(ats));
        given(obtainedUnitRepository.findPlanetIdsByExpirationId(OBTAINED_UNIT_TEMPORAL_INFORMATION_ID)).willReturn(planetIds);
        var ruleDto = givenRuleDto().toBuilder().destinationType(ObjectEnum.UNIT.name()).build();
        given(ruleBo.findByOriginTypeAndOriginIdAndType(timeSpecialObject.name(), TIME_SPECIAL_ID, TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID))
                .willReturn(List.of(ruleDto));
        given(objectRelationsRepository.findOneByObjectCodeAndReferenceId(timeSpecialObject.name(), TIME_SPECIAL_ID))
                .willReturn(or);
        var temporalUnit = givenObtainedUnitTemporalInformation(OBJECT_RELATION_ID);
        given(obtainedUnitTemporalInformationRepository.findByRelationId(OBJECT_RELATION_ID)).willReturn(List.of(temporalUnit));
        doAnswer(new InvokeRunnableLambdaAnswer(1)).when(planetLockUtilService).doInsideLockById(anyList(), any());
        var ou = givenObtainedUnit1();
        ou.setExpirationId(OBTAINED_UNIT_TEMPORAL_INFORMATION_ID);
        given(obtainedUnitRepository.findByExpirationId(OBTAINED_UNIT_TEMPORAL_INFORMATION_ID)).willReturn(List.of(ou));

        temporalUnitScheduleListener.relationLost(ur);

        verify(obtainedUnitRepository, times(timesInteraction)).deleteAll(List.of(ou));

    }

    private static Stream<Arguments> relationLost_should_delete_units_affected_by_time_special_lost_arguments() {
        return Stream.of(
                Arguments.of(Set.of(SOURCE_PLANET_ID), 1),
                Arguments.of(Set.of(), 0)
        );
    }
}
