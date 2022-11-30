package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.mission.GatherMissionResultDto;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.AdditionalMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenFaction;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenGatherMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = GatherMissionProcessor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ReturnMissionRegistrationBo.class,
        ImprovementBo.class,
        SocketIoService.class,
        TransactionUtilService.class,
        AttackMissionProcessor.class
})
class GatherMissionProcessorTest {
    private final GatherMissionProcessor gatherMissionProcessor;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final ImprovementBo improvementBo;
    private final SocketIoService socketIoService;
    private final TransactionUtilService transactionUtilService;
    private final AttackMissionProcessor attackMissionProcessor;

    @Autowired
    GatherMissionProcessorTest(
            GatherMissionProcessor gatherMissionProcessor,
            ReturnMissionRegistrationBo returnMissionRegistrationBo,
            ImprovementBo improvementBo,
            SocketIoService socketIoService,
            TransactionUtilService transactionUtilService,
            AttackMissionProcessor attackMissionProcessor
    ) {
        this.gatherMissionProcessor = gatherMissionProcessor;
        this.returnMissionRegistrationBo = returnMissionRegistrationBo;
        this.improvementBo = improvementBo;
        this.socketIoService = socketIoService;
        this.transactionUtilService = transactionUtilService;
        this.attackMissionProcessor = attackMissionProcessor;
    }

    @Test
    void supports_should_work() {
        assertThat(gatherMissionProcessor.supports(MissionType.EXPLORE)).isFalse();
        assertThat(gatherMissionProcessor.supports(MissionType.GATHER)).isTrue();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,null,10,210.0,210.0",
            "0,null,10,210.0,210.0",
            "0,0,10,210.0,210.0",
            "null,0,10,210.0,210.0",
            "10,0,10,210.0,210.0",
            "null,null,null,0,0",
            "null,60,5,105.0,105.0",
            "40,null,20,420.0,420.0",
            "40,60,5,84.0,126.0",
            "90,10,10,378.0,42.0"
    }, nullValues = "null")
    void process_should_work(
            Float customFactionPrimaryPercentage,
            Float customFactionSecondaryPercentage,
            Integer unitCharge,
            Double expectedPrimary,
            Double expectedSecondary
    ) {
        var mission = givenGatherMission();
        var userSpy = spy(givenUser1());
        var userFaction = givenFaction();
        userFaction.setCustomPrimaryGatherPercentage(customFactionPrimaryPercentage);
        userFaction.setCustomSecondaryGatherPercentage(customFactionSecondaryPercentage);
        userSpy.setFaction(userFaction);
        mission.setUser(userSpy);
        var targetPlanet = mission.getTargetPlanet();
        targetPlanet.setRichness(20);
        var ou = givenObtainedUnit1();
        ou.getUnit().setCharge(unitCharge);
        var involvedUnits = List.of(ou);
        double delta = 0.01D;
        var groupedImprovementMock = mock(GroupedImprovement.class);
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        given(attackMissionProcessor.triggerAttackIfRequired(mission, userSpy, targetPlanet)).willReturn(true);
        given(groupedImprovementMock.getMoreChargeCapacity()).willReturn(5F);
        given(improvementBo.findUserImprovement(userSpy)).willReturn(groupedImprovementMock);
        given(improvementBo.findAsRational(5F)).willReturn(20F);
        doNothing().when(userSpy).addtoPrimary(any());
        doNothing().when(userSpy).addToSecondary(any());
        given(reportBuilderMock.withGatherInformation(any(), any())).willReturn(reportBuilderMock);
        var supplierAnswer = new InvokeSupplierLambdaAnswer<GatherMissionResultDto>(2);
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        doAnswer(supplierAnswer).when(socketIoService)
                .sendMessage(eq(userSpy), eq("mission_gather_result"), any());

        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(() -> UnitMissionReportBuilder.create(userSpy, mission.getSourcePlanet(), targetPlanet, involvedUnits))
                    .thenReturn(reportBuilderMock);

            var retVal = gatherMissionProcessor.process(mission, involvedUnits);

            verify(returnMissionRegistrationBo, times(1)).registerReturnMission(mission, null);
            verify(userSpy, times(1)).addtoPrimary(AdditionalMatchers.eq(expectedPrimary, delta));
            verify(userSpy, times(1)).addToSecondary(AdditionalMatchers.eq(expectedSecondary, delta));
            var sent = supplierAnswer.getResult();
            assertThat(sent.getPrimaryResource()).isCloseTo(expectedPrimary, offset(delta));
            assertThat(sent.getSecondaryResource()).isCloseTo(expectedSecondary, offset(delta));
            verify(reportBuilderMock, times(1)).withGatherInformation(
                    AdditionalMatchers.eq(expectedPrimary, delta),
                    AdditionalMatchers.eq(expectedSecondary, delta)
            );
            assertThat(mission.getResolved()).isTrue();
            assertThat(retVal).isSameAs(reportBuilderMock);
        }
    }

    @Test
    void process_should_do_nothing_when_attack_killed_mission() {
        var mission = givenGatherMission();
        var user = givenUser1();
        mission.setUser(user);

        assertThat(gatherMissionProcessor.process(mission, List.of(givenObtainedUnit1()))).isNull();
        verify(attackMissionProcessor, times(1)).triggerAttackIfRequired(mission, user, mission.getTargetPlanet());
        verify(returnMissionRegistrationBo, never()).registerReturnMission(any(), any());
        verify(improvementBo, never()).findAsRational(any(Double.class));
        verify(improvementBo, never()).findUserImprovement(any());

    }
}
