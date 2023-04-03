package com.kevinguanchedarias.owgejava.business.mission.checker;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.GalaxyMock.givenGalaxy;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.SPEED_IMPACT_GROUP_ID;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        classes = CrossGalaxyMissionChecker.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        EntityCanDoMissionChecker.class,
        ObjectRelationBo.class,
        UnlockedRelationBo.class
})
class CrossGalaxyMissionCheckerTest {
    private static final int OTHER_GALAXY_ID = 11888;

    private final CrossGalaxyMissionChecker crossGalaxyMissionChecker;
    private final EntityCanDoMissionChecker entityCanDoMissionChecker;
    private final ObjectRelationBo objectRelationBo;
    private final UnlockedRelationBo unlockedRelationBo;

    @Autowired
    public CrossGalaxyMissionCheckerTest(
            CrossGalaxyMissionChecker crossGalaxyMissionChecker,
            EntityCanDoMissionChecker entityCanDoMissionChecker,
            ObjectRelationBo objectRelationBo,
            UnlockedRelationBo unlockedRelationBo
    ) {
        this.crossGalaxyMissionChecker = crossGalaxyMissionChecker;
        this.entityCanDoMissionChecker = entityCanDoMissionChecker;
        this.objectRelationBo = objectRelationBo;
        this.unlockedRelationBo = unlockedRelationBo;
    }

    @Test
    void checkCrossGalaxy_should_not_throw_when_source_and_target_planet_are_in_same_galaxy() {
        var ou = givenObtainedUnit1();
        ou.getUnit().setSpeedImpactGroup(givenSpeedImpactGroup());

        assertDoesNotThrow(() -> crossGalaxyMissionChecker.checkCrossGalaxy(MissionType.ATTACK, List.of(ou), givenSourcePlanet(), givenTargetPlanet()));

        verify(entityCanDoMissionChecker, never()).canDoMission(any(), any(), any(), any());
    }

    @Test
    void checkCrossGalaxy_should_not_throw_when_unit_has_not_speed_group() {
        var ou = givenObtainedUnit1();
        var targetPlanet = givenTargetPlanet();
        targetPlanet.setGalaxy(Galaxy.builder().id(OTHER_GALAXY_ID).build());

        assertDoesNotThrow(() -> crossGalaxyMissionChecker.checkCrossGalaxy(MissionType.ATTACK, List.of(ou), givenSourcePlanet(), targetPlanet));

        verify(entityCanDoMissionChecker, never()).canDoMission(any(), any(), any(), any());
    }

    @Test
    void checkCrossGalaxy_should_ignore_stored_units() {
        var spi = givenSpeedImpactGroup();
        var ou = givenObtainedUnit1();
        var user = ou.getUser();
        ou.getUnit().setSpeedImpactGroup(spi);
        var storedOu = givenObtainedUnit2().toBuilder().user(user).ownerUnit(ou).build();
        storedOu.getUnit().setSpeedImpactGroup(spi);
        var targetPlanetWithOtherGalaxy = givenTargetPlanet().toBuilder().galaxy(givenGalaxy(4)).build();
        given(entityCanDoMissionChecker.canDoMission(user, targetPlanetWithOtherGalaxy, spi, MissionType.EXPLORE)).willReturn(true);

        assertDoesNotThrow(() -> crossGalaxyMissionChecker.checkCrossGalaxy(
                MissionType.EXPLORE, List.of(ou, storedOu), givenSourcePlanet(), targetPlanetWithOtherGalaxy
        ));

        verify(entityCanDoMissionChecker, times(1))
                .canDoMission(user, targetPlanetWithOtherGalaxy, spi, MissionType.EXPLORE);
    }

    @ParameterizedTest
    @MethodSource("checkCrossGalaxy_should_throw_when_entity_can_not_do_mission_arguments")
    void checkCrossGalaxy_should_throw_when_entity_can_not_do_mission(
            SpeedImpactGroup expectedUsedSpeedImpactGroup,
            SpeedImpactGroup unitSpeedImpactGroup,
            SpeedImpactGroup unitTypeSpeedImpactGroup
    ) {
        var missionType = MissionType.ATTACK;
        var ou = givenObtainedUnit1();
        var user = ou.getUser();
        var ouList = List.of(ou);
        ou.getUnit().setSpeedImpactGroup(unitSpeedImpactGroup);
        ou.getUnit().getType().setSpeedImpactGroup(unitTypeSpeedImpactGroup);
        var sourcePlanet = givenSourcePlanet();
        var targetPlanet = givenTargetPlanet();
        targetPlanet.setGalaxy(Galaxy.builder().id(OTHER_GALAXY_ID).build());

        assertThatThrownBy(() ->
                crossGalaxyMissionChecker.checkCrossGalaxy(missionType, ouList, sourcePlanet, targetPlanet)
        )
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageStartingWith("This speed group")
                .hasMessageEndingWith("outside of the galaxy");

        verify(entityCanDoMissionChecker, times(1)).canDoMission(user, targetPlanet, expectedUsedSpeedImpactGroup, missionType);
        verify(objectRelationBo, never()).findOne(any(), any());
    }

    @Test
    void checkCrossGalaxy_should_not_throw_but_warn_when_relation_id_is_null(CapturedOutput capturedOutput) {
        var missionType = MissionType.ATTACK;
        var ou = givenObtainedUnit1();
        var user = ou.getUser();
        var ouList = List.of(ou);
        var speedImpactGroup = givenSpeedImpactGroup();
        ou.getUnit().setSpeedImpactGroup(speedImpactGroup);
        var sourcePlanet = givenSourcePlanet();
        var targetPlanet = givenTargetPlanet();
        targetPlanet.setGalaxy(Galaxy.builder().id(OTHER_GALAXY_ID).build());
        given(entityCanDoMissionChecker.canDoMission(user, targetPlanet, speedImpactGroup, missionType)).willReturn(true);

        assertDoesNotThrow(() -> crossGalaxyMissionChecker.checkCrossGalaxy(missionType, ouList, sourcePlanet, targetPlanet));

        verify(objectRelationBo, times(1)).findOne(ObjectEnum.SPEED_IMPACT_GROUP, SPEED_IMPACT_GROUP_ID);
        verify(unlockedRelationBo, never()).isUnlocked(any(), any());
        assertThat(capturedOutput.getOut()).contains("Unexpected null objectRelation for SPEED_IMPACT_GROUP");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkCrossGalaxy_should_throw_when_speed_group_is_not_unlocked(boolean isUnlocked) {
        var missionType = MissionType.ATTACK;
        var ou = givenObtainedUnit1();
        var user = ou.getUser();
        var ouList = List.of(ou);
        var speedImpactGroup = givenSpeedImpactGroup();
        ou.getUnit().setSpeedImpactGroup(speedImpactGroup);
        var sourcePlanet = givenSourcePlanet();
        var targetPlanet = givenTargetPlanet();
        var or = givenObjectRelation();
        targetPlanet.setGalaxy(Galaxy.builder().id(OTHER_GALAXY_ID).build());
        given(entityCanDoMissionChecker.canDoMission(user, targetPlanet, speedImpactGroup, missionType)).willReturn(true);
        given(objectRelationBo.findOne(ObjectEnum.SPEED_IMPACT_GROUP, SPEED_IMPACT_GROUP_ID)).willReturn(or);
        given(unlockedRelationBo.isUnlocked(user, or)).willReturn(isUnlocked);

        if (isUnlocked) {
            assertDoesNotThrow(() -> crossGalaxyMissionChecker.checkCrossGalaxy(missionType, ouList, sourcePlanet, targetPlanet));
        } else {
            assertThatThrownBy(() ->
                    crossGalaxyMissionChecker.checkCrossGalaxy(missionType, ouList, sourcePlanet, targetPlanet)
            )
                    .isInstanceOf(SgtBackendInvalidInputException.class)
                    .hasMessageStartingWith("Don't try it")
                    .hasMessageEndingWith("and you know it");
        }

        verify(unlockedRelationBo, times(1)).isUnlocked(user, or);

    }

    private static Stream<Arguments> checkCrossGalaxy_should_throw_when_entity_can_not_do_mission_arguments() {
        var speedImpactGroup = givenSpeedImpactGroup();
        return Stream.of(
                Arguments.of(speedImpactGroup, speedImpactGroup, null),
                Arguments.of(speedImpactGroup, null, speedImpactGroup)
        );
    }
}
