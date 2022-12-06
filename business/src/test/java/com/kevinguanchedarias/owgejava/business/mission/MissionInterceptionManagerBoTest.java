package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnitInterceptionFinderBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.InterceptedUnitsInformationMock.INTERCEPTED_UNIT;
import static com.kevinguanchedarias.owgejava.mock.InterceptedUnitsInformationMock.givenInterceptedUnitsInformation;
import static com.kevinguanchedarias.owgejava.mock.MissionInterceptionInformationMock.*;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionTypeMock.givenMissinType;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MissionInterceptionManagerBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UnitInterceptionFinderBo.class,
        ObtainedUnitRepository.class,
        MissionUnitsFinderBo.class,
        MissionReportManagerBo.class
})
class MissionInterceptionManagerBoTest {
    private final MissionInterceptionManagerBo missionInterceptionManagerBo;
    private final UnitInterceptionFinderBo unitInterceptionFinderBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final MissionUnitsFinderBo missionUnitsFinderBo;
    private final MissionReportManagerBo missionReportManagerBo;

    @Autowired
    public MissionInterceptionManagerBoTest(
            MissionInterceptionManagerBo missionInterceptionManagerBo,
            UnitInterceptionFinderBo unitInterceptionFinderBo,
            ObtainedUnitRepository obtainedUnitRepository,
            MissionUnitsFinderBo missionUnitsFinderBo,
            MissionReportManagerBo missionReportManagerBo
    ) {
        this.missionInterceptionManagerBo = missionInterceptionManagerBo;
        this.unitInterceptionFinderBo = unitInterceptionFinderBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.missionUnitsFinderBo = missionUnitsFinderBo;
        this.missionReportManagerBo = missionReportManagerBo;
    }

    @ParameterizedTest
    @MethodSource("loadInformation_should_work_arguments")
    void loadInformation_should_work(
            MissionType missionType,
            InterceptedUnitsInformation interceptedUnits,
            boolean isIntercepted,
            int totalInterceptedUnits,
            int timesFindUnitsInvolved,
            int timesDelete,
            List<InterceptedUnitsInformation> expectedInterceptedUnits
    ) {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        var involvedAfterInterception = givenObtainedUnit1().toBuilder().id(49L).build();
        given(missionUnitsFinderBo.findUnitsInvolved(EXPLORE_MISSION_ID))
                .willReturn(List.of(ou))
                .willReturn(List.of(involvedAfterInterception));
        given(unitInterceptionFinderBo.checkInterceptsSpeedImpactGroup(mission, List.of(ou)))
                .willReturn(interceptedUnits == null ? List.of() : List.of(interceptedUnits));

        var information = this.missionInterceptionManagerBo.loadInformation(mission, missionType);

        assertThat(information.isMissionIntercepted()).isEqualTo(isIntercepted);
        assertThat(information.getTotalInterceptedUnits()).isEqualTo(totalInterceptedUnits);
        assertThat(information.getInvolvedUnits())
                .contains(interceptedUnits == null || missionType == MissionType.RETURN_MISSION ? ou : involvedAfterInterception)
                .hasSize(1);
        assertThat(information.getOriginallyInvolved()).contains(ou).hasSize(1);
        assertThat(information.getInterceptedUnits()).containsAll(expectedInterceptedUnits).hasSize(expectedInterceptedUnits.size());
        verify(obtainedUnitRepository, times(timesDelete)).deleteAll(List.of(givenObtainedUnit1()));
        verify(missionUnitsFinderBo, times(timesFindUnitsInvolved)).findUnitsInvolved(EXPLORE_MISSION_ID);
    }

    @Test
    void loadInformation_should_do_nothing_for_return_mission() {
        var ou = givenObtainedUnit1();
        var mission = givenExploreMission();
        mission.setType(givenMissinType(MissionType.RETURN_MISSION));
        given(missionUnitsFinderBo.findUnitsInvolved(EXPLORE_MISSION_ID))
                .willReturn(List.of(ou));

        var information = missionInterceptionManagerBo.loadInformation(mission, MissionType.RETURN_MISSION);

        assertThat(information.isMissionIntercepted()).isFalse();
        assertThat(information.getTotalInterceptedUnits()).isZero();
        assertThat(information.getInvolvedUnits()).contains(ou);
        assertThat(information.getOriginallyInvolved()).contains(ou);
        assertThat(information.getInterceptedUnits()).isNull();

    }

    @Test
    void maybeAppendDataToMissionReport_should_work() {
        var mission = givenExploreMission();
        var interceptionInformation = givenMissionInterceptionInformation();
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);

        missionInterceptionManagerBo.maybeAppendDataToMissionReport(mission, reportBuilderMock, interceptionInformation);

        verify(reportBuilderMock, times(1)).withInvolvedUnits(INTERCEPTION_INFORMATION_ORIGINALLY);
        verify(reportBuilderMock, times(1)).withInterceptionInformation(INTERCEPTION_INFORMATION_INTERCEPTED);
        verify(unitInterceptionFinderBo, times(1))
                .sendReportToInterceptorUsers(INTERCEPTION_INFORMATION_INTERCEPTED, mission.getSourcePlanet(), mission.getTargetPlanet());

    }

    @Test
    void maybeAppendDataToMissionReport_should_do_nothing_when_null_builder() {
        var mission = givenExploreMission();
        var interceptionInformation = givenMissionInterceptionInformation();

        missionInterceptionManagerBo.maybeAppendDataToMissionReport(mission, null, interceptionInformation);

        verify(unitInterceptionFinderBo, never()).sendReportToInterceptorUsers(any(), any(), any());
    }

    @Test
    void maybeAppendDataToMissionReport_should_do_nothing_when_zero_interception() {
        var mission = givenExploreMission();
        var interceptionInformation = givenMissionInterceptionInformation().toBuilder().totalInterceptedUnits(0).build();
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);

        missionInterceptionManagerBo.maybeAppendDataToMissionReport(mission, reportBuilderMock, interceptionInformation);

        verify(reportBuilderMock, never()).withInvolvedUnits(any());
        verify(reportBuilderMock, never()).withInterceptionInformation(any());
        verify(unitInterceptionFinderBo, never()).sendReportToInterceptorUsers(any(), any(), any());
    }

    @Test
    void handleMissionInterception_should_work() {
        var mission = givenExploreMission();
        mission.setUser(givenUser1());
        var interceptionInformation = givenMissionInterceptionInformation();
        var builderMock = mock(UnitMissionReportBuilder.class);
        given(builderMock.withInterceptionInformation(INTERCEPTION_INFORMATION_INTERCEPTED)).willReturn(builderMock);

        try (var builderStaticMock = mockStatic(UnitMissionReportBuilder.class)) {
            var targetPlanet = mission.getTargetPlanet();
            var sourcePlanet = mission.getSourcePlanet();
            builderStaticMock.when(
                    () -> UnitMissionReportBuilder.create(
                            mission.getUser(), sourcePlanet, targetPlanet, INTERCEPTION_INFORMATION_ORIGINALLY
                    )
            ).thenReturn(builderMock);

            missionInterceptionManagerBo.handleMissionInterception(mission, interceptionInformation);

            assertThat(mission.getResolved()).isTrue();
            verify(obtainedUnitRepository, times(1)).deleteAll(List.of(INTERCEPTED_UNIT));
            verify(builderMock, times(1)).withInterceptionInformation(INTERCEPTION_INFORMATION_INTERCEPTED);
            verify(missionReportManagerBo, times(1)).handleMissionReportSave(mission, builderMock);
            verify(unitInterceptionFinderBo, times(1)).sendReportToInterceptorUsers(INTERCEPTION_INFORMATION_INTERCEPTED, sourcePlanet, targetPlanet);
        }

    }

    private static Stream<Arguments> loadInformation_should_work_arguments() {
        var interceptedUnits = givenInterceptedUnitsInformation(Set.of(givenObtainedUnit1()));
        return Stream.of(
                Arguments.of(MissionType.EXPLORE, interceptedUnits, true, 1, 2, 1, List.of(interceptedUnits)),
                Arguments.of(MissionType.EXPLORE, null, false, 0, 1, 0, List.of())
        );
    }
}
