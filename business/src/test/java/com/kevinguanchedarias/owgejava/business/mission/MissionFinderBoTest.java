package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.MissionInformation;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.mock.MissionMock;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.*;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_COUNT;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.SOURCE_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = MissionFinderBo.class
)
@MockBean({
        MissionRepository.class,
        ObtainedUnitRepository.class,
        MissionTypeBo.class,
        PlanetRepository.class,
        ObjectRelationBo.class
})
class MissionFinderBoTest {
    private final MissionFinderBo missionFinderBo;
    private final MissionRepository missionRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final MissionTypeBo missionTypeBo;
    private final ObjectRelationBo objectRelationBo;
    private final PlanetRepository planetRepository;

    @Autowired
    MissionFinderBoTest(
            MissionFinderBo missionFinderBo,
            MissionRepository missionRepository,
            MissionTypeBo missionTypeBo,
            ObtainedUnitRepository obtainedUnitRepository,
            ObjectRelationBo objectRelationBo,
            PlanetRepository planetRepository
    ) {
        this.missionFinderBo = missionFinderBo;
        this.missionRepository = missionRepository;
        this.missionTypeBo = missionTypeBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.objectRelationBo = objectRelationBo;
        this.planetRepository = planetRepository;
    }

    @Test
    void findDeployedMissionOrCreate_should_merge_new_deployment_with_existing_old_for_unit_group() {
        var ou = givenObtainedUnit1();
        var existingMission = MissionMock.givenDeployedMission();
        existingMission.setInvolvedUnits(new ArrayList<>());
        when(missionRepository.findByUserIdAndTypeCodeAndTargetPlanetIdAndResolvedFalse(
                USER_ID_1, MissionType.DEPLOYED.name(), ou.getTargetPlanet().getId()
        )).thenReturn(List.of(existingMission));

        var result = missionFinderBo.findDeployedMissionOrCreate(ou);

        verify(missionRepository, times(1)).findByUserIdAndTypeCodeAndTargetPlanetIdAndResolvedFalse(
                USER_ID_1, MissionType.DEPLOYED.name(), ou.getTargetPlanet().getId()
        );
        var captor = ArgumentCaptor.forClass(ObtainedUnit.class);
        verify(this.obtainedUnitRepository, times(1)).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved).isEqualTo(ou);
        assertThat(saved.getMission()).isEqualTo(existingMission);
        verify(missionTypeBo, never()).find(any());
        verify(missionRepository, never()).save(any());
        verify(missionRepository, never()).findById(any());
        assertThat(result).isEqualTo(existingMission);
        assertThat(existingMission.getInvolvedUnits())
                .hasSize(1)
                .contains(ou);
    }

    @Test
    void findDeployedMissionOrCreate_should_create_new_mission_when_not_exists_nor_has_first_deployment() {
        var ou = givenObtainedUnit1();
        var missionType = givenMissionType(MissionType.DEPLOYED);
        when(missionTypeBo.find(MissionType.DEPLOYED)).thenReturn(missionType);
        when(missionRepository.save(any())).thenAnswer(returnsFirstArg());
        when(obtainedUnitRepository.save(any())).thenAnswer(returnsFirstArg());

        var result = missionFinderBo.findDeployedMissionOrCreate(ou);

        var captor = ArgumentCaptor.forClass(Mission.class);
        verify(missionRepository, times(1)).save(captor.capture());
        var savedMission = captor.getValue();
        assertThat(savedMission.getType()).isEqualTo(missionType);
        assertThat(savedMission.getUser()).isEqualTo(ou.getUser());
        assertThat(savedMission.getSourcePlanet()).isEqualTo(ou.getSourcePlanet());
        assertThat(savedMission.getTargetPlanet()).isEqualTo(ou.getTargetPlanet());
        var unitCaptor = ArgumentCaptor.forClass(ObtainedUnit.class);
        verify(obtainedUnitRepository, times(1)).save(unitCaptor.capture());
        assertThat(result).isEqualTo(savedMission);
    }

    @Test
    void findRunningUnitBuild_should_do_nothing_on_null_mission() {
        assertThat(missionFinderBo.findRunningUnitBuild(USER_ID_1, 14D)).isNull();

        verifyNoInteractions(objectRelationBo, planetRepository, obtainedUnitRepository);
    }

    @Test
    void findRunningUnitBuild_should_work() {
        var mission = givenBuildMission();
        var planet = mission.getSourcePlanet();
        var or = givenObjectRelation();
        var missionInformation = MissionInformation.builder().relation(or).build();
        mission.setMissionInformation(missionInformation);
        var ou = givenObtainedUnit1();
        var unit = givenUnit1();
        double planetId = SOURCE_PLANET_ID;
        given(missionRepository.findByUserIdAndTypeCodeAndMissionInformationValue(USER_ID_1, MissionType.BUILD_UNIT.name(), planetId))
                .willReturn(mission);
        given(objectRelationBo.unboxObjectRelation(or)).willReturn(unit);
        given(planetRepository.findById(SOURCE_PLANET_ID)).willReturn(Optional.of(planet));
        given(obtainedUnitRepository.findByMissionId(BUILD_MISSION_ID)).willReturn(List.of(ou));

        var retVal = this.missionFinderBo.findRunningUnitBuild(USER_ID_1, planetId);

        assertThat(retVal.getUnit().getId()).isEqualTo(UNIT_ID_1);
        assertThat(retVal.getMissionId()).isEqualTo(BUILD_MISSION_ID);
        assertThat(retVal.getSourcePlanet().getId()).isEqualTo(SOURCE_PLANET_ID);
        assertThat(retVal.getCount()).isEqualTo(OBTAINED_UNIT_1_COUNT);

    }

    @ParameterizedTest
    @MethodSource("findBuildMissions_should_work_arguments")
    void findBuildMissions_should_work(List<ObtainedUnit> ouList, Long expectedCount) {
        var mission = givenBuildMission();
        var planet = mission.getSourcePlanet();
        var or = givenObjectRelation();
        var missionInformation = MissionInformation.builder().relation(or).value((double) SOURCE_PLANET_ID).build();
        mission.setMissionInformation(missionInformation);
        var unit = givenUnit1();
        given(missionRepository.findByUserIdAndTypeCodeAndResolvedFalse(USER_ID_1, MissionType.BUILD_UNIT.name()))
                .willReturn(List.of(mission));
        given(objectRelationBo.unboxObjectRelation(or)).willReturn(unit);
        given(planetRepository.findById(SOURCE_PLANET_ID)).willReturn(Optional.of(planet));
        given(obtainedUnitRepository.findByMissionId(BUILD_MISSION_ID)).willReturn(ouList);

        var retVal = this.missionFinderBo.findBuildMissions(USER_ID_1);

        assertThat(retVal).hasSize(1);
        var entry = retVal.getFirst();
        assertThat(entry.getUnit().getId()).isEqualTo(UNIT_ID_1);
        assertThat(entry.getMissionId()).isEqualTo(BUILD_MISSION_ID);
        assertThat(entry.getSourcePlanet().getId()).isEqualTo(SOURCE_PLANET_ID);
        assertThat(entry.getCount()).isEqualTo(expectedCount);
    }

    private static Stream<Arguments> findBuildMissions_should_work_arguments() {
        return Stream.of(
                Arguments.of(List.of(givenObtainedUnit1()), OBTAINED_UNIT_1_COUNT),
                Arguments.of(List.of(), 0L)
        );
    }
}
