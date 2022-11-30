package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.RETURN_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenReturnMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.SOURCE_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = ReturnMissionProcessor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetLockUtilService.class,
        ObtainedUnitRepository.class,
        ObtainedUnitBo.class,
        AsyncRunnerBo.class,
        ObtainedUnitEventEmitter.class,
        MissionEventEmitterBo.class
})
class ReturnMissionProcessorTest {
    private final ReturnMissionProcessor returnMissionProcessor;
    private final PlanetLockUtilService planetLockUtilService;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObtainedUnitBo obtainedUnitBo;
    private final AsyncRunnerBo asyncRunnerBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;

    @Autowired
    ReturnMissionProcessorTest(
            ReturnMissionProcessor returnMissionProcessor,
            PlanetLockUtilService planetLockUtilService,
            ObtainedUnitRepository obtainedUnitRepository,
            ObtainedUnitBo obtainedUnitBo,
            AsyncRunnerBo asyncRunnerBo,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            MissionEventEmitterBo missionEventEmitterBo
    ) {
        this.returnMissionProcessor = returnMissionProcessor;
        this.planetLockUtilService = planetLockUtilService;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.obtainedUnitBo = obtainedUnitBo;
        this.asyncRunnerBo = asyncRunnerBo;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.missionEventEmitterBo = missionEventEmitterBo;
    }

    @Test
    void supports_should_work() {
        assertThat(returnMissionProcessor.supports(MissionType.EXPLORE)).isFalse();
        assertThat(returnMissionProcessor.supports(MissionType.RETURN_MISSION)).isTrue();
    }

    @Test
    void process_should_work() {
        var mission = givenReturnMission();
        var user = givenUser1();
        mission.setUser(user);
        var ou = givenObtainedUnit1();
        doAnswer(new InvokeRunnableLambdaAnswer(1)).when(planetLockUtilService)
                .doInsideLock(eq(List.of(mission.getSourcePlanet(), mission.getTargetPlanet())), any());
        given(obtainedUnitRepository.findByMissionId(RETURN_MISSION_ID)).willReturn(List.of(ou));
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(asyncRunnerBo).runAssyncWithoutContextDelayed(any(), eq(500L));

        assertThat(returnMissionProcessor.process(mission, null)).isNull();

        verify(obtainedUnitBo, times(1)).moveUnit(ou, USER_ID_1, SOURCE_PLANET_ID);
        assertThat(mission.getResolved()).isTrue();
        verify(missionEventEmitterBo, times(1)).emitLocalMissionChangeAfterCommit(mission);
        verify(obtainedUnitEventEmitter, times(1)).emitObtainedUnits(user);

    }
}
