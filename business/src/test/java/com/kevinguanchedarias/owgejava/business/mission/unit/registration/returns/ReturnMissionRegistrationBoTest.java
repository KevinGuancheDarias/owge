package com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns;

import com.kevinguanchedarias.owgejava.business.MissionSchedulerService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTimeManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTypeBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionTypeMock.givenMissinType;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = ReturnMissionRegistrationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionTypeBo.class,
        MissionTimeManagerBo.class,
        MissionRepository.class,
        MissionSchedulerService.class,
        MissionEventEmitterBo.class,
        ObtainedUnitRepository.class,
        PlanetLockUtilService.class
})
class ReturnMissionRegistrationBoTest {
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final MissionTypeBo missionTypeBo;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final MissionRepository missionRepository;
    private final MissionSchedulerService missionSchedulerService;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final PlanetLockUtilService planetLockUtilService;

    @Autowired
    ReturnMissionRegistrationBoTest(
            ReturnMissionRegistrationBo returnMissionRegistrationBo,
            MissionTypeBo missionTypeBo,
            MissionTimeManagerBo missionTimeManagerBo,
            MissionRepository missionRepository,
            MissionSchedulerService missionSchedulerService,
            MissionEventEmitterBo missionEventEmitterBo,
            ObtainedUnitRepository obtainedUnitRepository,
            PlanetLockUtilService planetLockUtilService
    ) {
        this.returnMissionRegistrationBo = returnMissionRegistrationBo;
        this.missionTypeBo = missionTypeBo;
        this.missionTimeManagerBo = missionTimeManagerBo;
        this.missionRepository = missionRepository;
        this.missionSchedulerService = missionSchedulerService;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.planetLockUtilService = planetLockUtilService;
    }

    @ParameterizedTest
    @CsvSource(value = {
            "60,30,60,false",
            "null,48,48,true"
    }, nullValues = "null")
    void registerReturnMission_should(
            Double customRequiredTime, Double originalRequiredTime, Double expectedRequiredTime, boolean isInvisible
    ) {
        var originalMission = givenExploreMission();
        var user = givenUser1();
        var sourcePlanet = originalMission.getSourcePlanet();
        var targetPlanet = originalMission.getTargetPlanet();
        originalMission.setUser(user);
        originalMission.setInvisible(isInvisible);
        var terminationDate = LocalDateTime.now();
        var missionTypeEntity = givenMissinType(MissionType.RETURN_MISSION);
        var ou = givenObtainedUnit1();
        originalMission.setRequiredTime(originalRequiredTime);
        given(missionTypeBo.find(MissionType.RETURN_MISSION)).willReturn(missionTypeEntity);
        given(missionTimeManagerBo.computeTerminationDate(expectedRequiredTime)).willReturn(terminationDate);
        given(obtainedUnitRepository.findByMissionId(EXPLORE_MISSION_ID)).willReturn(List.of(ou));
        doAnswer(new InvokeRunnableLambdaAnswer(1)).when(planetLockUtilService).doInsideLock(eq(List.of(sourcePlanet, targetPlanet)), any());

        returnMissionRegistrationBo.registerReturnMission(originalMission, customRequiredTime);

        var captor = ArgumentCaptor.forClass(Mission.class);
        verify(missionRepository, times(1)).saveAndFlush(captor.capture());
        var savedReturnMission = captor.getValue();
        assertThat(savedReturnMission.getStartingDate()).isNotNull();
        assertThat(savedReturnMission.getType()).isEqualTo(missionTypeEntity);
        assertThat(savedReturnMission.getRequiredTime()).isEqualTo(expectedRequiredTime);
        assertThat(savedReturnMission.getTerminationDate()).isEqualTo(terminationDate);
        assertThat(savedReturnMission.getSourcePlanet()).isEqualTo(sourcePlanet);
        assertThat(savedReturnMission.getTargetPlanet()).isEqualTo(targetPlanet);
        assertThat(savedReturnMission.getUser()).isEqualTo(user);
        assertThat(savedReturnMission.getRelatedMission()).isEqualTo(originalMission);
        assertThat(savedReturnMission.getInvisible()).isEqualTo(isInvisible);
        assertThat(ou.getMission()).isEqualTo(savedReturnMission);
        verify(obtainedUnitRepository, times(1)).saveAll(List.of(ou));
        verify(missionSchedulerService, times(1)).scheduleMission(savedReturnMission);
        verify(missionEventEmitterBo, times(1)).emitLocalMissionChangeAfterCommit(savedReturnMission);
    }
}
