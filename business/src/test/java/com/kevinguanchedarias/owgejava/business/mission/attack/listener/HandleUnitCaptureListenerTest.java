package com.kevinguanchedarias.owgejava.business.mission.attack.listener;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.rule.UnitRuleFinderService;
import com.kevinguanchedarias.owgejava.business.rule.type.unit.UnitCaptureRuleTypeProviderBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.mock.MissionMock;
import com.kevinguanchedarias.owgejava.pojo.attack.listener.UnitCaptureContext;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.business.mission.attack.listener.HandleUnitCaptureListener.CAPTURE_UNIT_CONTEXT_NAME;
import static com.kevinguanchedarias.owgejava.mock.AttackMock.*;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenAttackMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenUnitCaptureRule;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit2;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = HandleUnitCaptureListener.class
)
@MockBean({
        RuleBo.class,
        ObtainedUnitBo.class,
        MissionFinderBo.class,
        MissionReportBo.class,
        ObtainedUnitRepository.class,
        UnitRuleFinderService.class
})
class HandleUnitCaptureListenerTest {

    private final HandleUnitCaptureListener handleUnitCaptureListener;
    private final RuleBo ruleBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionFinderBo missionFinderBo;
    private final MissionReportBo missionReportBo;
    private final UnitRuleFinderService unitRuleFinderService;

    @Autowired
    HandleUnitCaptureListenerTest(
            HandleUnitCaptureListener handleUnitCaptureListener,
            RuleBo ruleBo,
            ObtainedUnitBo obtainedUnitBo,
            MissionFinderBo missionFinderBo,
            MissionReportBo missionReportBo,
            UnitRuleFinderService unitRuleFinderService
    ) {
        this.handleUnitCaptureListener = handleUnitCaptureListener;
        this.ruleBo = ruleBo;
        this.obtainedUnitBo = obtainedUnitBo;
        this.missionFinderBo = missionFinderBo;
        this.missionReportBo = missionReportBo;
        this.unitRuleFinderService = unitRuleFinderService;
    }

    @Test
    void onAfterUnitKilledCalculation_should_use_attack_mission_info_when_attacker_unit_has_mission() {
        var attacker = givenAttackObtainedUnit();
        var attackerOu = attacker.getObtainedUnit();
        attacker.getObtainedUnit().setMission(givenAttackMission());
        var killed = 1L;
        var deployedMission = MissionMock.givenDeployedMission();
        int probability = 100;
        mockRule(attackerOu.getUnit(), attackerOu.getUnit(), probability);
        int captureAmountPercentage = 50;
        when(missionFinderBo.findDeployedMissionOrCreate(any()))
                .thenReturn(deployedMission);
        when(this.ruleBo.hasExtraArg(any(Rule.class), anyInt())).thenReturn(true);
        when(this.ruleBo.findExtraArgs(any())).thenReturn(List.of(Integer.toString(probability), Integer.toString(captureAmountPercentage)));

        handleUnitCaptureListener.onAfterUnitKilledCalculation(givenAttackInformation(), attacker, attacker, killed);

        var captor = ArgumentCaptor.forClass(ObtainedUnit.class);
        verify(obtainedUnitBo, times(1)).moveUnit(
                captor.capture(),
                eq(USER_ID_1),
                eq(TARGET_PLANET_ID)
        );
        verify(ruleBo, times(2)).hasExtraArg(any(Rule.class), anyInt());
        verify(unitRuleFinderService, never()).findRuleByActiveTimeSpecialsAndTargetUnit(any(), any(), any());
        var capturedUnit = captor.getValue();
        assertThat(capturedUnit.getSourcePlanet()).isEqualTo(givenSourcePlanet());
        assertThat(capturedUnit.getTargetPlanet()).isEqualTo(givenTargetPlanet());
    }

    @Test
    void onAfterUnitKilledCalculation_should_not_try_to_save_if_not_rule() {
        var attacker = givenAttackObtainedUnit();
        var killed = 1L;

        handleUnitCaptureListener.onAfterUnitKilledCalculation(givenAttackInformation(), attacker, attacker, killed);

        verify(ruleBo, never()).findExtraArg(any(Rule.class), anyInt());
        verify(unitRuleFinderService, times(1)).findRuleByActiveTimeSpecialsAndTargetUnit(any(), any(), any());
    }

    @Test
    void onAfterUnitKilledCalculation_should_try_to_save_rule_and_probability() {
        var attacker = givenAttackObtainedUnit();
        var victim = givenAttackObtainedUnit(givenObtainedUnit2());
        var killed = 4L;
        int probability = 100;
        var captureRule = givenUnitCaptureRule(probability);
        int captureAmountPercentage = 50;
        var information = givenAttackInformation();
        when(ruleBo.findExtraArgs(captureRule)).thenReturn(List.of(Integer.toString(probability), Integer.toString(captureAmountPercentage)));
        when(ruleBo.hasExtraArg(eq(captureRule), anyInt())).thenReturn(true);
        mockRule(attacker.getObtainedUnit().getUnit(), victim.getObtainedUnit().getUnit(), probability);

        handleUnitCaptureListener.onAfterUnitKilledCalculation(information, attacker, victim, killed);

        verify(ruleBo, times(1)).hasExtraArg(captureRule, 0);
        verify(ruleBo, times(1)).hasExtraArg(captureRule, 1);
        var captor = ArgumentCaptor.forClass(ObtainedUnit.class);
        verify(obtainedUnitBo, times(1)).moveUnit(captor.capture(), eq(USER_ID_1), eq(SOURCE_PLANET_ID));
        var capturedUnit = captor.getValue();
        assertThat(information.getContextData())
                .containsKey(CAPTURE_UNIT_CONTEXT_NAME);
        var capturedUnitsContextInfo = information.getContextData(CAPTURE_UNIT_CONTEXT_NAME, UnitCaptureContext.class).get(0);
        assertThat(capturedUnitsContextInfo.getCapturedUnits()).isBetween(1L, 4L);
        assertThat(capturedUnitsContextInfo.getCaptorUnit()).isEqualTo(attacker);
        assertThat(capturedUnitsContextInfo.getVictimUnit()).isEqualTo(victim);

        assertThat(capturedUnit.getUnit().getId()).isEqualTo(givenUnit2().getId());
        assertThat(capturedUnit.getUser().getId()).isEqualTo(givenUser1().getId());
        assertThat(capturedUnit.getCount()).isBetween(1L, 4L);
        assertThat(capturedUnit.isFromCapture()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void onAttackEnd_should_work() {
        try (var builderStaticMock = mockStatic(UnitMissionReportBuilder.class)) {
            var informationSpy = spy(givenFullAttackInformation());
            var builderMock = mock(UnitMissionReportBuilder.class);
            var entry = UnitCaptureContext.builder()
                    .captorUnit(givenAttackObtainedUnit())
                    .victimUnit(givenAttackObtainedUnit(givenObtainedUnit2()))
                    .capturedUnits(8L)
                    .build();
            var contextData = List.of(entry, entry);
            builderStaticMock.when(() -> UnitMissionReportBuilder.create(any(), any(), any(), any())).thenReturn(builderMock);
            when(informationSpy.getContextData(CAPTURE_UNIT_CONTEXT_NAME, UnitCaptureContext.class)).thenReturn(contextData);

            handleUnitCaptureListener.onAttackEnd(informationSpy);

            verify(informationSpy, times(1)).getContextData(CAPTURE_UNIT_CONTEXT_NAME, UnitCaptureContext.class);
            ArgumentCaptor<List<UnitCaptureContext>> captor = ArgumentCaptor.forClass(List.class);
            verify(builderMock, times(1)).withUnitCaptureInformation(captor.capture());
            var sentToReport = captor.getValue();
            assertThat(sentToReport).hasSize(2);
            assertThat(sentToReport.get(0)).isEqualTo(entry);
            verify(missionReportBo, times(1)).create(builderMock, false, givenUser1());
        }
    }

    private void mockRule(Unit from, Unit to, int captureProbability) {
        given(unitRuleFinderService.findRule(UnitCaptureRuleTypeProviderBo.PROVIDER_ID, from, to)).willReturn(Optional.of(givenUnitCaptureRule(captureProbability)));
    }

}
