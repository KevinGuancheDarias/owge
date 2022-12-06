package com.kevinguanchedarias.owgejava.business.mission.processor;


import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.GlobalConstants.MAX_PLANETS_MESSAGE;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenEstablishBaseMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = EstablishBaseMissionProcessor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        AttackMissionProcessor.class,
        ReturnMissionRegistrationBo.class,
        PlanetBo.class,
        MissionEventEmitterBo.class
})
class EstablishBaseMissionProcessorTest {
    private final EstablishBaseMissionProcessor establishBaseMissionProcessor;
    private final AttackMissionProcessor attackMissionProcessor;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final PlanetBo planetBo;
    private final MissionEventEmitterBo missionEventEmitterBo;

    @Autowired
    public EstablishBaseMissionProcessorTest(
            EstablishBaseMissionProcessor establishBaseMissionProcessor,
            AttackMissionProcessor attackMissionProcessor,
            ReturnMissionRegistrationBo returnMissionRegistrationBo,
            PlanetBo planetBo,
            MissionEventEmitterBo missionEventEmitterBo
    ) {
        this.establishBaseMissionProcessor = establishBaseMissionProcessor;
        this.attackMissionProcessor = attackMissionProcessor;
        this.returnMissionRegistrationBo = returnMissionRegistrationBo;
        this.planetBo = planetBo;
        this.missionEventEmitterBo = missionEventEmitterBo;
    }

    @Test
    void supports_should_work() {
        assertThat(establishBaseMissionProcessor.supports(MissionType.EXPLORE)).isFalse();
        assertThat(establishBaseMissionProcessor.supports(MissionType.ESTABLISH_BASE)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("process_should_handle_mission_continue_after_trigger_attack_arguments")
    void process_should_handle_mission_continue_after_trigger_attack(
            UserStorage planetOwner,
            boolean hasMaxPlanets,
            int timesReturnMission,
            int timesFailedReportInfo,
            int timesSuccessReportInfo,
            String expectedMessage
    ) {
        var mission = givenEstablishBaseMission();
        var user = givenUser1();
        mission.setUser(user);
        var targetPlanet = mission.getTargetPlanet();
        targetPlanet.setOwner(planetOwner);
        var involvedUnits = List.of(givenObtainedUnit1());
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        given(attackMissionProcessor.triggerAttackIfRequired(mission, user, targetPlanet)).willReturn(true);
        given(planetBo.hasMaxPlanets(user)).willReturn(hasMaxPlanets);

        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(() -> UnitMissionReportBuilder.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits))
                    .thenReturn(reportBuilderMock);

            var retVal = establishBaseMissionProcessor.process(mission, involvedUnits);

            verify(returnMissionRegistrationBo, times(timesReturnMission)).registerReturnMission(mission, null);
            verify(reportBuilderMock, times(timesFailedReportInfo)).withEstablishBaseInformation(false, expectedMessage);
            verify(reportBuilderMock, times(timesSuccessReportInfo)).withEstablishBaseInformation(true);
            verify(planetBo, times(timesSuccessReportInfo)).definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
            assertThat(mission.getResolved()).isTrue();
            verify(missionEventEmitterBo, times(1)).emitLocalMissionChangeAfterCommit(mission);
            assertThat(retVal).isSameAs(reportBuilderMock);
        }
    }

    @Test
    void process_should_do_nothing_when_attack_killed_the_mission() {
        var mission = givenEstablishBaseMission();
        var user = givenUser1();
        mission.setUser(user);

        assertThat(establishBaseMissionProcessor.process(mission, List.of())).isNull();

        verify(attackMissionProcessor, times(1)).triggerAttackIfRequired(mission, user, mission.getTargetPlanet());
        verify(planetBo, never()).hasMaxPlanets(any());
        verify(missionEventEmitterBo, never()).emitLocalMissionChangeAfterCommit(any());
    }

    private static Stream<Arguments> process_should_handle_mission_continue_after_trigger_attack_arguments() {
        var planetOwner = givenUser2();
        var expectedHasOwnerMessage = "I18N_ALREADY_HAS_OWNER";
        return Stream.of(
                Arguments.of(planetOwner, true, 1, 1, 0, expectedHasOwnerMessage),
                Arguments.of(null, true, 1, 1, 0, MAX_PLANETS_MESSAGE),
                Arguments.of(planetOwner, false, 1, 1, 0, expectedHasOwnerMessage),
                Arguments.of(null, false, 0, 0, 1, null)
        );
    }
}
