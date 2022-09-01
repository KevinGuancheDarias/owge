package com.kevinguanchedarias.owgejava.business.speedimpactgroup;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.AllianceBo;
import com.kevinguanchedarias.owgejava.business.MissionReportBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.InterceptedUnitsInformationMock.INTERCEPTOR_UNIT;
import static com.kevinguanchedarias.owgejava.mock.InterceptedUnitsInformationMock.INTERCEPTOR_USER;
import static com.kevinguanchedarias.owgejava.mock.InterceptedUnitsInformationMock.givenInterceptedUnitsInformation;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionReportMock.givenReport;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnitWithInterception1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = UnitInterceptionFinderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUnitBo.class,
        SpeedImpactGroupBo.class,
        AllianceBo.class,
        MissionReportBo.class
})
class UnitInterceptionFinderBoTest {
    private final UnitInterceptionFinderBo unitInterceptionFinderBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final SpeedImpactGroupBo speedImpactGroupBo;
    private final AllianceBo allianceBo;
    private final MissionReportBo missionReportBo;

    @Autowired
    public UnitInterceptionFinderBoTest(
            UnitInterceptionFinderBo unitInterceptionFinderBo,
            ObtainedUnitBo obtainedUnitBo,
            SpeedImpactGroupBo speedImpactGroupBo,
            AllianceBo allianceBo,
            MissionReportBo missionReportBo
    ) {
        this.unitInterceptionFinderBo = unitInterceptionFinderBo;
        this.obtainedUnitBo = obtainedUnitBo;
        this.speedImpactGroupBo = speedImpactGroupBo;
        this.allianceBo = allianceBo;
        this.missionReportBo = missionReportBo;
    }

    @ParameterizedTest
    @CsvSource({
            "false,0",
            "true,1"
    })
    void checkInterceptsSpeedImpactGroup_should_work(boolean areEnemies, int times) {
        var mission = givenExploreMission();
        var planet = mission.getTargetPlanet();
        var attackerInterceptedUnit = givenObtainedUnit1();
        var attackerInterceptedUnit2 = givenObtainedUnit1().toBuilder()
                .id(999887L)
                .count(28L)
                .unit(attackerInterceptedUnit.getUnit())
                .build();
        var interceptedUser = attackerInterceptedUnit.getUser();
        var interceptorUnit = givenObtainedUnit2();
        var unitWithInterception = givenUnitWithInterception1();
        var groupThatCanIntercept = unitWithInterception.getInterceptableSpeedGroups();
        var interceptorUnitWithInterception = givenObtainedUnit2().toBuilder()
                .unit(unitWithInterception)
                .build();
        var userThatIntercepted = interceptorUnitWithInterception.getUser();
        given(obtainedUnitBo.findInvolvedInAttack(planet)).willReturn(List.of(attackerInterceptedUnit, attackerInterceptedUnit2, interceptorUnit, interceptorUnitWithInterception, interceptorUnitWithInterception));
        given(speedImpactGroupBo.canIntercept(groupThatCanIntercept, interceptedUser, attackerInterceptedUnit.getUnit())).willReturn(true);
        given(allianceBo.areEnemies(userThatIntercepted, interceptedUser)).willReturn(areEnemies);

        var result = unitInterceptionFinderBo.checkInterceptsSpeedImpactGroup(mission, List.of(attackerInterceptedUnit, attackerInterceptedUnit2));

        assertThat(result).hasSize(times);
        if (areEnemies) {
            var entry = result.get(0);
            assertThat(entry.getInterceptedUnits()).contains(attackerInterceptedUnit);
            assertThat(entry.getInterceptorUnit()).isEqualTo(interceptorUnitWithInterception);
            assertThat(entry.getInterceptorUser()).isEqualTo(userThatIntercepted);
        }
    }

    @Test
    void sendReportToInterceptorUsers_should_work() {
        var interceptedInformation = givenInterceptedUnitsInformation();
        var createdReport = givenReport();
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        var sourcePlanet = givenSourcePlanet();
        var targetPlanet = givenTargetPlanet();
        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(() -> UnitMissionReportBuilder.create(INTERCEPTOR_USER, sourcePlanet, targetPlanet, List.of(INTERCEPTOR_UNIT)))
                    .thenReturn(reportBuilderMock);
            given(missionReportBo.create(reportBuilderMock, true, INTERCEPTOR_USER)).willReturn(createdReport);

            unitInterceptionFinderBo.sendReportToInterceptorUsers(List.of(interceptedInformation), sourcePlanet, targetPlanet);

            verify(reportBuilderMock, times(1)).withInterceptionInformation(List.of(interceptedInformation));
            verify(missionReportBo, times(1)).create(reportBuilderMock, true, INTERCEPTOR_USER);
        }

    }
}
