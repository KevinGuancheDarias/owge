package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = ExploreMissionProcessor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetExplorationService.class,
        ObtainedUnitBo.class,
        ReturnMissionRegistrationBo.class
})
class ExploreMissionProcessorTest {
    private final ExploreMissionProcessor exploreMissionProcessor;
    private final PlanetExplorationService planetExplorationService;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;

    @Autowired
    public ExploreMissionProcessorTest(
            ExploreMissionProcessor exploreMissionProcessor,
            PlanetExplorationService planetExplorationService,
            ObtainedUnitBo obtainedUnitBo,
            ReturnMissionRegistrationBo returnMissionRegistrationBo
    ) {
        this.exploreMissionProcessor = exploreMissionProcessor;
        this.planetExplorationService = planetExplorationService;
        this.obtainedUnitBo = obtainedUnitBo;
        this.returnMissionRegistrationBo = returnMissionRegistrationBo;
    }

    @Test
    void supports_should_work() {
        assertThat(exploreMissionProcessor.supports(MissionType.GATHER)).isFalse();
        assertThat(exploreMissionProcessor.supports(MissionType.EXPLORE)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "false,1",
            "true,0"
    })
    void process_should_work(boolean isExplored, int timesDefineAsExplored) {
        var mission = givenExploreMission();
        var user = givenUser1();
        mission.setUser(user);
        var targetPlanet = mission.getTargetPlanet();
        var involvedUnits = List.of(givenObtainedUnit1());
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        var unitsInPlanet = List.of(mock(ObtainedUnitDto.class));
        given(planetExplorationService.isExplored(user, targetPlanet)).willReturn(isExplored);
        given(obtainedUnitBo.explorePlanetUnits(mission, targetPlanet)).willReturn(unitsInPlanet);
        given(reportBuilderMock.withExploredInformation(unitsInPlanet)).willReturn(reportBuilderMock);

        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(() -> UnitMissionReportBuilder.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits))
                    .thenReturn(reportBuilderMock);

            var retVal = exploreMissionProcessor.process(mission, involvedUnits);

            verify(planetExplorationService, times(timesDefineAsExplored)).defineAsExplored(user, targetPlanet);
            verify(returnMissionRegistrationBo, times(1)).registerReturnMission(mission, null);
            assertThat(mission.getResolved()).isTrue();
            assertThat(retVal).isSameAs(reportBuilderMock);
        }
    }
}
