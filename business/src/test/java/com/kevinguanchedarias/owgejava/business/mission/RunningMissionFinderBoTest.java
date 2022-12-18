package com.kevinguanchedarias.owgejava.business.mission;


import com.kevinguanchedarias.owgejava.business.planet.PlanetCleanerService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.mock.MissionMock;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.AllianceMock.givenAlliance;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.*;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.*;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = RunningMissionFinderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionRepository.class,
        PlanetRepository.class,
        ObtainedUnitRepository.class,
        HiddenUnitBo.class,
        PlanetExplorationService.class,
        UserStorageRepository.class,
        ObtainedUnitFinderBo.class,
        PlanetCleanerService.class
})
class RunningMissionFinderBoTest {
    private final RunningMissionFinderBo runningMissionFinderBo;
    private final MissionRepository missionRepository;
    private final PlanetExplorationService planetExplorationService;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final HiddenUnitBo hiddenUnitBo;
    private final PlanetRepository planetRepository;
    private final UserStorageRepository userStorageRepository;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;
    private final PlanetCleanerService planetCleanerService;

    @Autowired
    public RunningMissionFinderBoTest(
            RunningMissionFinderBo runningMissionFinderBo,
            MissionRepository missionRepository,
            PlanetExplorationService planetExplorationService,
            ObtainedUnitRepository obtainedUnitRepository,
            HiddenUnitBo hiddenUnitBo,
            PlanetRepository planetRepository,
            UserStorageRepository userStorageRepository,
            ObtainedUnitFinderBo obtainedUnitFinderBo,
            PlanetCleanerService planetCleanerService
    ) {
        this.runningMissionFinderBo = runningMissionFinderBo;
        this.missionRepository = missionRepository;
        this.planetExplorationService = planetExplorationService;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.hiddenUnitBo = hiddenUnitBo;
        this.planetRepository = planetRepository;
        this.userStorageRepository = userStorageRepository;
        this.obtainedUnitFinderBo = obtainedUnitFinderBo;
        this.planetCleanerService = planetCleanerService;
    }

    @Test
    void findEnemyRunningMissions_should_work() {
        var mission1Id = 192091L;
        var mission2Id = 29892L;
        var userPlanet1 = Planet.builder().id(14L).build();
        var userPlanet2 = Planet.builder().id(19L).build();
        var user = givenUser1();
        var user2 = givenUser2();
        var exploredPlanet = Planet.builder().id(190L).build();
        var involvedUnit = givenObtainedUnit1();
        var invisibleInvolvedUnit = givenObtainedUnit2();
        invisibleInvolvedUnit.getUnit().setIsInvisible(true);
        var involvedUnits = List.of(involvedUnit, invisibleInvolvedUnit);
        var missionWithExploredPlanet = Mission.builder()
                .id(mission1Id)
                .sourcePlanet(exploredPlanet)
                .targetPlanet(givenTargetPlanet())
                .user(user2)
                .involvedUnits(involvedUnits)
                .type(MissionMock.givenMissionType(MissionType.EXPLORE))
                .build();
        var missionWithoutExploredPlanet = Mission.builder()
                .id(mission2Id)
                .sourcePlanet(Planet.builder().id(40L).build())
                .targetPlanet(givenTargetPlanet())
                .user(user2)
                .involvedUnits(involvedUnits)
                .type(MissionMock.givenMissionType(MissionType.EXPLORE))
                .build();
        given(missionRepository.findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(any(), any()))
                .willReturn(List.of(missionWithExploredPlanet, missionWithoutExploredPlanet));
        given(planetRepository.findByOwnerId(USER_ID_1)).willReturn(List.of(userPlanet1, userPlanet2));
        given(planetExplorationService.isExplored(user, exploredPlanet)).willReturn(true);
        given(obtainedUnitRepository.findByMissionId(anyLong())).willReturn(involvedUnits);

        var result = runningMissionFinderBo.findEnemyRunningMissions(user);

        verify(missionRepository, times(1))
                .findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(List.of(userPlanet1, userPlanet2), user);
        verify(planetExplorationService, times(2)).isExplored(eq(user), any());
        assertThat(result).hasSize(2);
        var missionResult1 = result.get(0);
        var missionResult2 = result.get(1);
        assertThat(missionResult1.getSourcePlanet()).isNotNull();
        assertThat(missionResult1.getUser()).isNotNull();
        assertThat(missionResult2.getSourcePlanet()).isNull();
        assertThat(missionResult2.getUser()).isNull();
        Stream.concat(missionResult1.getInvolvedUnits().stream(), missionResult2.getInvolvedUnits().stream()).forEach(involved -> {
            assertThat(involved.getSourcePlanet()).isNull();
            assertThat(involved.getTargetPlanet()).isNull();
        });
        var units = missionResult1.getInvolvedUnits();
        var visibleUnitResult = units.get(0);
        var invisibleUnitResult = units.get(1);
        assertThat(visibleUnitResult.getUnit()).isNotNull();
        assertThat(visibleUnitResult.getCount()).isEqualTo(OBTAINED_UNIT_1_COUNT);
        assertThat(invisibleUnitResult.getUnit()).isNull();
        assertThat(invisibleUnitResult.getCount()).isNull();
        verify(hiddenUnitBo, times(2)).defineHidden(eq(involvedUnits), anyList());
    }

    @Test
    void countUserRunningMissions_should_work() {
        var count = 4;
        given(missionRepository.countByUserIdAndResolvedFalse(USER_ID_1)).willReturn(count);

        assertThat(runningMissionFinderBo.countUserRunningMissions(USER_ID_1)).isEqualTo(count);
    }

    @Test
    void findUserRunningMissions_should_work_with_gather() {
        var mission = givenGatherMission();
        var user = givenUser1();
        var ou = givenObtainedUnit1();
        var ouDtoMock = mock(ObtainedUnitDto.class);
        given(missionRepository.findByUserIdAndResolvedFalse(USER_ID_1)).willReturn(List.of(mission));
        given(userStorageRepository.getReferenceById(USER_ID_1)).willReturn(user);
        given(obtainedUnitRepository.findByMissionId(any())).willReturn(List.of(ou));
        given(obtainedUnitFinderBo.findCompletedAsDto(user, List.of(ou))).willReturn(List.of(ouDtoMock));
        try (var mockedConstructor = mockConstruction(UnitRunningMissionDto.class)) {
            runningMissionFinderBo.findUserRunningMissions(USER_ID_1);

            var dto = mockedConstructor.constructed().get(0);

            verify(dto, times(1)).setInvolvedUnits(List.of(ouDtoMock));
            verify(planetCleanerService, never()).cleanUpUnexplored(any(), any());
            verify(dto, times(1)).nullifyInvolvedUnitsPlanets();
        }
    }

    @Test
    void findUserRunningMissions_should_work_with_explore() {
        var mission = givenExploreMission();
        var user = givenUser1();
        mission.setUser(user);
        user.setAlliance(givenAlliance());
        var ou = givenObtainedUnit1();
        var ouDtoMock = mock(ObtainedUnitDto.class);
        given(missionRepository.findByUserIdAndResolvedFalse(USER_ID_1)).willReturn(List.of(mission));
        given(userStorageRepository.getReferenceById(USER_ID_1)).willReturn(user);
        given(obtainedUnitRepository.findByMissionId(EXPLORE_MISSION_ID)).willReturn(List.of(ou));
        given(obtainedUnitFinderBo.findCompletedAsDto(user, List.of(ou))).willReturn(List.of(ouDtoMock));

        var result = runningMissionFinderBo.findUserRunningMissions(USER_ID_1);

        verify(planetCleanerService, times(1)).cleanUpUnexplored(eq(USER_ID_1), any(PlanetDto.class));
        assertThat(result).hasSize(1);
        var resultEntry = result.get(0);
        assertThat(resultEntry.getInvolvedUnits()).containsExactly(ouDtoMock);
    }
}
