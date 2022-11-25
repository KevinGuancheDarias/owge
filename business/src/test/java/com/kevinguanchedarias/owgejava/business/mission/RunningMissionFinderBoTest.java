package com.kevinguanchedarias.owgejava.business.mission;


import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
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

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.*;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = RunningMissionFinderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionRepository.class,
        PlanetRepository.class,
        ObtainedUnitRepository.class,
        HiddenUnitBo.class,
        PlanetBo.class,
        UserStorageRepository.class,
        ObtainedUnitFinderBo.class
})
class RunningMissionFinderBoTest {
    private final RunningMissionFinderBo runningMissionFinderBo;
    private final MissionRepository missionRepository;
    private final PlanetBo planetBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final HiddenUnitBo hiddenUnitBo;
    private final PlanetRepository planetRepository;

    @Autowired
    public RunningMissionFinderBoTest(
            RunningMissionFinderBo runningMissionFinderBo,
            MissionRepository missionRepository,
            PlanetBo planetBo,
            ObtainedUnitRepository obtainedUnitRepository,
            HiddenUnitBo hiddenUnitBo,
            PlanetRepository planetRepository
    ) {
        this.runningMissionFinderBo = runningMissionFinderBo;
        this.missionRepository = missionRepository;
        this.planetBo = planetBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.hiddenUnitBo = hiddenUnitBo;
        this.planetRepository = planetRepository;
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
        given(planetBo.isExplored(user, exploredPlanet)).willReturn(true);
        given(obtainedUnitRepository.findByMissionId(anyLong())).willReturn(involvedUnits);

        var result = runningMissionFinderBo.findEnemyRunningMissions(user);

        verify(missionRepository, times(1))
                .findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(List.of(userPlanet1, userPlanet2), user);
        verify(planetBo, times(2)).isExplored(eq(user), any());
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
}
