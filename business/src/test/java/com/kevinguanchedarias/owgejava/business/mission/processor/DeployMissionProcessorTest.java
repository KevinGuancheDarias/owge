package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionUnitsFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.*;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = DeployMissionProcessor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionUnitsFinderBo.class,
        ObtainedUnitBo.class,
        TransactionUtilService.class,
        ObtainedUnitEventEmitter.class,
        MissionEventEmitterBo.class,
        EntityManager.class,
        HiddenUnitBo.class
})
class DeployMissionProcessorTest {
    private final DeployMissionProcessor deployMissionProcessor;
    private final MissionUnitsFinderBo missionUnitsFinderBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final TransactionUtilService transactionUtilService;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final EntityManager entityManager;
    private final HiddenUnitBo hiddenUnitBo;

    @Autowired
    DeployMissionProcessorTest(
            DeployMissionProcessor deployMissionProcessor,
            MissionUnitsFinderBo missionUnitsFinderBo,
            ObtainedUnitBo obtainedUnitBo,
            TransactionUtilService transactionUtilService,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            MissionEventEmitterBo missionEventEmitterBo,
            EntityManager entityManager,
            HiddenUnitBo hiddenUnitBo
    ) {
        this.deployMissionProcessor = deployMissionProcessor;
        this.missionUnitsFinderBo = missionUnitsFinderBo;
        this.obtainedUnitBo = obtainedUnitBo;
        this.transactionUtilService = transactionUtilService;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.entityManager = entityManager;
        this.hiddenUnitBo = hiddenUnitBo;
    }

    @Test
    void supports_should_work() {
        assertThat(deployMissionProcessor.supports(MissionType.EXPLORE)).isFalse();
        assertThat(deployMissionProcessor.supports(MissionType.DEPLOY)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("process_should_work_arguments")
    void process_should_work(
            ObtainedUnit partnerUnit,
            Mission deployedMission,
            int timesHiddenUnit,
            boolean isHidden,
            UserStorage planetOwner,
            int timesEmitObtainedUnitToOwnerIfSameUser
    ) {
        var mission = givenDeployMission();
        var user = givenUser1();
        mission.setUser(user);
        mission.getTargetPlanet().setOwner(planetOwner);
        var ou = givenObtainedUnit1();
        var alteredUnit = ou.toBuilder().id(4L).mission(deployedMission).build();
        given(missionUnitsFinderBo.findUnitsInvolved(DEPLOY_MISSION_ID)).willReturn(List.of(ou));
        given(obtainedUnitBo.moveUnit(ou, USER_ID_1, TARGET_PLANET_ID)).willReturn(alteredUnit);
        given(hiddenUnitBo.isHiddenUnit(partnerUnit)).willReturn(isHidden);
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());

        var retVal = deployMissionProcessor.process(mission, List.of(ou));

        assertThat(retVal).isNull();

        verify(hiddenUnitBo, times(timesHiddenUnit)).isHiddenUnit(partnerUnit);
        if (deployedMission != null) {
            assertThat(deployedMission.getInvisible()).isEqualTo(isHidden);
        }
        assertThat(mission.getResolved()).isTrue();
        verify(entityManager, times(1)).refresh(alteredUnit);
        verify(obtainedUnitEventEmitter, times(timesEmitObtainedUnitToOwnerIfSameUser)).emitObtainedUnits(user);
        verify(missionEventEmitterBo, times(1)).emitLocalMissionChange(mission, USER_ID_1);
    }

    private static Stream<Arguments> process_should_work_arguments() {
        var alteredUnitPartnerInMission = givenObtainedUnit1().toBuilder().id(9L).build();
        var deployedMission = givenDeployedMission();
        deployedMission.setInvolvedUnits(List.of(alteredUnitPartnerInMission));
        var planetOwner = givenUser2();
        return Stream.of(
                Arguments.of(alteredUnitPartnerInMission, deployedMission, 1, true, planetOwner, 0),
                Arguments.of(alteredUnitPartnerInMission, deployedMission, 1, false, planetOwner, 0),
                Arguments.of(alteredUnitPartnerInMission, deployedMission, 1, false, null, 0),
                Arguments.of(alteredUnitPartnerInMission, deployedMission, 1, false, givenUser1(), 1),
                Arguments.of(null, null, 0, false, givenUser1(), 1)
        );
    }
}
