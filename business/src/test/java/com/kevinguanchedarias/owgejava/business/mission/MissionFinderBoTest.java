package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.mock.MissionMock;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenMissionType;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenRawMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
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


    @Autowired
    MissionFinderBoTest(
            MissionFinderBo missionFinderBo,
            MissionRepository missionRepository,
            MissionTypeBo missionTypeBo,
            ObtainedUnitRepository obtainedUnitRepository
    ) {
        this.missionFinderBo = missionFinderBo;
        this.missionRepository = missionRepository;
        this.missionTypeBo = missionTypeBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
    }

    @Test
    void findDeployedMissionOrCreate_should_merge_new_deployment_with_existing_old_for_unit_group() {
        var ou = givenObtainedUnit1();
        ou.setFirstDeploymentMission(new Mission());
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
        assertThat(saved.getFirstDeploymentMission()).isNull();
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
        var savedUnit = unitCaptor.getValue();
        assertThat(savedUnit.getFirstDeploymentMission()).isEqualTo(savedMission);
        assertThat(result).isEqualTo(savedMission);
    }

    @Test
    void findDeployedMissionOrCreate_should_create_new_mission_and_use_first_deployment_coordinates() {
        var ou = givenObtainedUnit1();
        var firstDeploymentSourcePlanet = new Planet();
        var firstDeploymentTargetPlanet = new Planet();
        var firstDeploymentMission = givenRawMission(firstDeploymentSourcePlanet, firstDeploymentTargetPlanet);
        var missionType = givenMissionType(MissionType.DEPLOYED);
        ou.setFirstDeploymentMission(firstDeploymentMission);

        when(missionTypeBo.find(MissionType.DEPLOYED)).thenReturn(missionType);
        when(missionRepository.save(any())).thenAnswer(returnsFirstArg());
        when(obtainedUnitRepository.save(any())).thenAnswer(returnsFirstArg());
        when(missionRepository.findById(any())).thenReturn(Optional.of(firstDeploymentMission));

        var result = missionFinderBo.findDeployedMissionOrCreate(ou);

        var captor = ArgumentCaptor.forClass(Mission.class);
        verify(missionRepository, times(1)).save(captor.capture());
        var savedMission = captor.getValue();
        assertThat(savedMission.getType()).isEqualTo(missionType);
        assertThat(savedMission.getUser()).isEqualTo(ou.getUser());
        assertThat(savedMission.getSourcePlanet()).isEqualTo(firstDeploymentSourcePlanet);
        assertThat(savedMission.getTargetPlanet()).isEqualTo(firstDeploymentTargetPlanet);
        verify(obtainedUnitRepository, never()).save(any());
        assertThat(result).isEqualTo(savedMission);

    }

}
