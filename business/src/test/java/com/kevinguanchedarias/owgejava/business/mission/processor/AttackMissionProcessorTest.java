package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.audit.AuditBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.attack.AttackMissionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.AllianceMock.givenAlliance;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.INITIAL_COUNT;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenFullAttackInformation;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenAttackMission;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = AttackMissionProcessor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUnitRepository.class,
        AttackMissionManagerBo.class,
        MissionReportManagerBo.class,
        ReturnMissionRegistrationBo.class,
        AuditBo.class,
        MissionEventEmitterBo.class,
        RequirementBo.class
})
class AttackMissionProcessorTest {
    private final AttackMissionProcessor attackMissionProcessor;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final AttackMissionManagerBo attackMissionManagerBo;
    private final MissionReportManagerBo missionReportManagerBo;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final AuditBo auditBo;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final RequirementBo requirementBo;

    @Autowired
    public AttackMissionProcessorTest(
            AttackMissionProcessor attackMissionProcessor,
            ObtainedUnitRepository obtainedUnitRepository,
            AttackMissionManagerBo attackMissionManagerBo,
            MissionReportManagerBo missionReportManagerBo,
            ReturnMissionRegistrationBo returnMissionRegistrationBo,
            AuditBo auditBo,
            MissionEventEmitterBo missionEventEmitterBo,
            RequirementBo requirementBo
    ) {
        this.attackMissionProcessor = attackMissionProcessor;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.attackMissionManagerBo = attackMissionManagerBo;
        this.missionReportManagerBo = missionReportManagerBo;
        this.returnMissionRegistrationBo = returnMissionRegistrationBo;
        this.auditBo = auditBo;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.requirementBo = requirementBo;
    }

    @Test
    void supports_should_work() {
        assertThat(attackMissionProcessor.supports(MissionType.EXPLORE)).isFalse();
        assertThat(attackMissionProcessor.supports(MissionType.ATTACK)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "true,false,0,true,true",
            "false,true,0,true,true",
            "false,false,0,true,true",
            "true,true,1,true,false",
            "true,true,1,false,true"
    })
    void triggerAttackIfRequired_should_work(
            boolean isAttackTriggerEnabled,
            boolean areUnitsInvolved,
            int timesHandleAttack,
            boolean missionIsRemoved,
            boolean expectedContinue
    ) {
        var mission = givenAttackMission();
        var targetPlanet = mission.getTargetPlanet();
        var user = givenUser1();
        var userAlliance = givenAlliance();
        var attackInformationMock = mock(AttackInformation.class);
        user.setAlliance(userAlliance);
        given(attackMissionManagerBo.isAttackTriggerEnabledForMission(MissionType.ATTACK)).willReturn(isAttackTriggerEnabled);
        given(obtainedUnitRepository.areUnitsInvolved(USER_ID_1, userAlliance, TARGET_PLANET_ID)).willReturn(areUnitsInvolved);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission)).willReturn(attackInformationMock);
        given(attackInformationMock.isRemoved()).willReturn(missionIsRemoved);

        var retVal = attackMissionProcessor.triggerAttackIfRequired(mission, user, targetPlanet);

        assertThat(retVal).isEqualTo(expectedContinue);
        verify(attackMissionManagerBo, times(timesHandleAttack)).startAttack(attackInformationMock);
    }

    @ParameterizedTest
    @MethodSource("processAttack_should_work_arguments")
    void processAttack_should_work(
            boolean survivorsDoReturn,
            boolean missionIsRemoved,
            int timesReturnMission,
            Set<Integer> usersWithDeleteMissions,
            UserStorage planetOwner,
            int timesEmitLocal
    ) {
        var mission = givenAttackMission();
        var targetPlanet = mission.getTargetPlanet();
        targetPlanet.setOwner(planetOwner);
        var user = givenUser1();
        var enemyUser = givenUser2();
        var attackInformationSpy = spy(givenFullAttackInformation());
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        attackInformationSpy.setRemoved(missionIsRemoved);
        given(attackMissionManagerBo.buildAttackInformation(targetPlanet, mission)).willReturn(attackInformationSpy);
        given(attackInformationSpy.getUsersWithDeletedMissions()).willReturn(usersWithDeleteMissions);
        given(reportBuilderMock.withAttackInformation(attackInformationSpy)).willReturn(reportBuilderMock);

        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(() -> UnitMissionReportBuilder.create(user, mission.getSourcePlanet(), targetPlanet, List.of()))
                    .thenReturn(reportBuilderMock);
            var retVal = attackMissionProcessor.processAttack(mission, survivorsDoReturn);

            assertThat(retVal.getReportBuilder()).isEqualTo(reportBuilderMock);
            assertThat(mission.getResolved()).isTrue();
            verify(attackMissionManagerBo, times(1)).startAttack(attackInformationSpy);
            verify(returnMissionRegistrationBo, times(timesReturnMission)).registerReturnMission(mission, null);
            verify(missionReportManagerBo, times(1)).handleMissionReportSave(mission, reportBuilderMock, true, List.of(enemyUser));
            verify(auditBo, times(1)).nonRequestAudit(AuditActionEnum.ATTACK_INTERACTION, null, user, USER_ID_2);
            verify(missionEventEmitterBo, times(timesEmitLocal)).emitLocalMissionChangeAfterCommit(mission);
        }
    }

    @ParameterizedTest
    @CsvSource({
            INITIAL_COUNT + ",0,0",
            "2,0,1",
            "0,1,0"
    })
    void process_should_handle_requirements_trigger(long finalCount, int timesTriggerKilled, int timesTriggerAmountChanged) {
        var mission = givenAttackMission();
        var user = givenUser1();
        var attackInformation = givenFullAttackInformation();
        var aou = attackInformation.getUnits().get(0);
        aou.setFinalCount(finalCount);
        var unit = aou.getObtainedUnit().getUnit();

        given(attackMissionManagerBo.buildAttackInformation(mission.getTargetPlanet(), mission)).willReturn(attackInformation);

        attackMissionProcessor.process(mission, null);

        verify(attackMissionManagerBo, times(1)).startAttack(attackInformation);
        verify(requirementBo, times(timesTriggerKilled)).triggerUnitBuildCompletedOrKilled(user, unit);
        verify(requirementBo, times(timesTriggerAmountChanged)).triggerUnitAmountChanged(user, unit);

    }

    private static Stream<Arguments> processAttack_should_work_arguments() {
        return Stream.of(
                Arguments.of(true, false, 1, Set.of(), null, 0),
                Arguments.of(true, true, 0, Set.of(), null, 1),
                Arguments.of(false, true, 0, Set.of(), null, 1),
                Arguments.of(false, false, 0, Set.of(USER_ID_2), null, 0),
                Arguments.of(false, false, 0, Set.of(USER_ID_2), null, 0),
                Arguments.of(false, true, 0, Set.of(USER_ID_2), null, 1),
                Arguments.of(false, true, 0, Set.of(USER_ID_2), givenUser2(), 1),
                Arguments.of(false, false, 0, Set.of(USER_ID_2), null, 0),
                Arguments.of(false, false, 0, Set.of(USER_ID_2), givenUser2(), 1),
                Arguments.of(false, false, 0, Set.of(), givenUser2(), 0)
        );
    }
}
