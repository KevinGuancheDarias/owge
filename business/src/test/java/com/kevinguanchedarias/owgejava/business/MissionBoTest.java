package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionConfigurationBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.mock.MissionMock;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_COUNT;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = MissionBo.class
)
@MockBean({
        MissionRepository.class,
        ObtainedUpgradeBo.class,
        ObjectRelationBo.class,
        UpgradeBo.class,
        UserStorageBo.class,
        MissionTypeRepository.class,
        ImprovementBo.class,
        RequirementBo.class,
        UnlockedRelationBo.class,
        UnitBo.class,
        ObtainedUnitBo.class,
        PlanetBo.class,
        ExceptionUtilService.class,
        UnitTypeBo.class,
        MissionConfigurationBo.class,
        SocketIoService.class,
        MissionReportBo.class,
        MissionSchedulerService.class,
        EntityManager.class,
        ConfigurationBo.class,
        AsyncRunnerBo.class,
        TransactionUtilService.class,
        TaggableCacheManager.class,
        HiddenUnitBo.class
})
class MissionBoTest {
    private final MissionBo missionBo;
    private final PlanetBo planetBo;
    private final MissionRepository missionRepository;
    private final SocketIoService socketIoService;
    private final HiddenUnitBo hiddenUnitBo;

    @Autowired
    public MissionBoTest(
            MissionBo missionBo,
            PlanetBo planetBo,
            MissionRepository missionRepository,
            SocketIoService socketIoService,
            HiddenUnitBo hiddenUnitBo
    ) {
        this.missionBo = missionBo;
        this.planetBo = planetBo;
        this.missionRepository = missionRepository;
        this.socketIoService = socketIoService;
        this.hiddenUnitBo = hiddenUnitBo;
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
        given(planetBo.findPlanetsByUser(user)).willReturn(List.of(userPlanet1, userPlanet2));
        given(planetBo.isExplored(user, exploredPlanet)).willReturn(true);

        var result = missionBo.findEnemyRunningMissions(user);

        verify(planetBo, times(1)).findPlanetsByUser(user);
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

    @SuppressWarnings("unchecked")
    @Test
    void emitEnemyMissionsChange_should_work() {
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
        given(planetBo.findPlanetsByUser(user)).willReturn(List.of(userPlanet1, userPlanet2));
        given(planetBo.isExplored(user, exploredPlanet)).willReturn(true);
        List<List<UnitRunningMissionDto>> messageResultContainer = new ArrayList<>();
        doAnswer(answer -> {
            messageResultContainer.add((List<UnitRunningMissionDto>) answer.getArgument(2, Supplier.class).get());
            return null;
        }).when(socketIoService).sendMessage(any(UserStorage.class), any(), any());

        missionBo.emitEnemyMissionsChange(user);

        var result = messageResultContainer.get(0);
        verify(planetBo, times(1)).findPlanetsByUser(user);
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
    }
}
