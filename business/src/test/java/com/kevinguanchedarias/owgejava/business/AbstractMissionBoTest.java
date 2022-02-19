package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionConfigurationBo;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.fake.FakeMissionBo;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.ATTACK_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.DEPLOYED_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenAttackMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenDeployedMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenMissionType;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = FakeMissionBo.class
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
})
class AbstractMissionBoTest {
    private final FakeMissionBo fakeMissionBo;
    private final SocketIoService socketIoService;
    private final MissionRepository missionRepository;
    private final ObtainedUnitBo obtainedUnitBo;
    private final PlanetBo planetBo;
    private final MissionTypeRepository missionTypeRepository;

    @Autowired
    AbstractMissionBoTest(
            FakeMissionBo fakeMissionBo,
            SocketIoService socketIoService,
            MissionRepository missionRepository,
            ObtainedUnitBo obtainedUnitBo,
            PlanetBo planetBo,
            MissionTypeRepository missionTypeRepository
    ) {
        this.fakeMissionBo = fakeMissionBo;
        this.socketIoService = socketIoService;
        this.missionRepository = missionRepository;
        this.obtainedUnitBo = obtainedUnitBo;
        this.planetBo = planetBo;
        this.missionTypeRepository = missionTypeRepository;
    }

    @Test
    void emitUnitMissions_should_work() {
        var missionsCount = 2;
        var missions = List.of(givenAttackMission(), givenDeployedMission());
        var unitsInMission = List.of(givenObtainedUnit1());
        given(missionRepository.countByUserIdAndResolvedFalse(USER_ID_1)).willReturn(missionsCount);
        given(missionRepository.findByUserIdAndResolvedFalse(USER_ID_1)).willReturn(missions);
        given(obtainedUnitBo.findByMissionId(ATTACK_MISSION_ID)).willReturn(unitsInMission);
        var messageResultContainer = new ArrayList<MissionWebsocketMessage>();
        doAnswer(answer -> {
            messageResultContainer.add((MissionWebsocketMessage) answer.getArgument(2, Supplier.class).get());
            return null;
        }).when(socketIoService).sendMessage(anyInt(), any(), any());

        fakeMissionBo.emitUnitMissions(USER_ID_1);

        verify(missionRepository, times(1)).countByUserIdAndResolvedFalse(USER_ID_1);
        verify(missionRepository, times(1)).findByUserIdAndResolvedFalse(USER_ID_1);
        verify(obtainedUnitBo, times(1)).findByMissionId(ATTACK_MISSION_ID);
        verify(obtainedUnitBo, times(1)).findByMissionId(DEPLOYED_MISSION_ID);
        verify(planetBo, never()).cleanUpUnexplored(anyInt(), any(PlanetDto.class));
        verify(socketIoService, times(1)).sendMessage(eq(USER_ID_1), eq("unit_mission_change"), any());
        var sentMessage = messageResultContainer.get(0);
        assertThat(sentMessage.getCount()).isEqualTo(missionsCount);
        assertThat(sentMessage.getMyUnitMissions()).hasSize(2);
    }

    @Test
    void findMissionType_should_work() {
        MissionType enumType = MissionType.EXPLORE;
        com.kevinguanchedarias.owgejava.entity.MissionType entityType = givenMissionType(enumType);
        when(missionTypeRepository.findOneByCode(enumType.name())).thenReturn(Optional.of(entityType));

        assertThat(fakeMissionBo.findMissionType(enumType)).isEqualTo(entityType);
    }

    @Test
    void findMissionType_should_throw_when_missing() {
        assertThatThrownBy(() -> fakeMissionBo.findMissionType(MissionType.EXPLORE))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("No MissionType")
                .hasMessageEndingWith("was found in the database");
    }
}
