package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.mission.checker.EntityCanDoMissionChecker;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MissionRegistrationUnitTypeChecker.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UnitTypeBo.class,
        SpeedImpactGroupFinderBo.class,
        EntityCanDoMissionChecker.class,
})
class MissionRegistrationUnitTypeCheckerTest {
    private final MissionRegistrationUnitTypeChecker missionRegistrationUnitTypeChecker;
    private final UnitTypeBo unitTypeBo;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;
    private final EntityCanDoMissionChecker entityCanDoMissionChecker;

    @Autowired
    MissionRegistrationUnitTypeCheckerTest(
            MissionRegistrationUnitTypeChecker missionRegistrationUnitTypeChecker,
            UnitTypeBo unitTypeBo,
            SpeedImpactGroupFinderBo speedImpactGroupFinderBo,
            EntityCanDoMissionChecker entityCanDoMissionChecker
    ) {
        this.missionRegistrationUnitTypeChecker = missionRegistrationUnitTypeChecker;
        this.unitTypeBo = unitTypeBo;
        this.speedImpactGroupFinderBo = speedImpactGroupFinderBo;
        this.entityCanDoMissionChecker = entityCanDoMissionChecker;
    }

    @Test
    void checkUnitsCanDoMission_should_throw() {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        var user = ou.getUser();
        var ouList = List.of(ou);

        assertThatThrownBy(() -> missionRegistrationUnitTypeChecker.checkUnitsCanDoMission(ouList, user, mission, MissionType.EXPLORE))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("don't try it dear hacker");

        verify(entityCanDoMissionChecker, never()).canDoMission(any(), any(), any(), any());
    }

    @Test
    void checkUnitCanDoMission_should_throw_when_spi_cant_do_mission() {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        var ouStored = givenObtainedUnit2().toBuilder().user(ou.getUser()).build();
        ouStored.setOwnerUnit(ou);
        ouStored.getUnit().setName("FOO_STORED_UNIT");
        var user = ou.getUser();
        var unitTypeEntity = ou.getUnit().getType();
        var spi = givenSpeedImpactGroup();
        var targetPlanet = mission.getTargetPlanet();
        var ouList = List.of(ou, ouStored);
        given(unitTypeBo.canDoMission(user, targetPlanet, List.of(unitTypeEntity, ouStored.getUnit().getType()), MissionType.EXPLORE)).willReturn(true);
        given(speedImpactGroupFinderBo.findApplicable(eq(user), any())).willReturn(spi);

        assertThatThrownBy(() -> missionRegistrationUnitTypeChecker.checkUnitsCanDoMission(ouList, user, mission, MissionType.EXPLORE))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("At least one unit speed group")
                .hasMessageContaining(spi.getName())
                .hasMessageContaining(ou.getUnit().getName())
                .hasMessageNotContaining(ouStored.getUnit().getName());
    }

    @Test
    void checkUnitCanDoMission_should_not_throw() {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        var user = ou.getUser();
        var unitTypeEntity = ou.getUnit().getType();
        var spi = givenSpeedImpactGroup();
        var targetPlanet = mission.getTargetPlanet();
        given(unitTypeBo.canDoMission(user, targetPlanet, List.of(unitTypeEntity), MissionType.EXPLORE)).willReturn(true);
        given(speedImpactGroupFinderBo.findApplicable(user, ou.getUnit())).willReturn(spi);
        given(entityCanDoMissionChecker.canDoMission(user, targetPlanet, spi, MissionType.EXPLORE)).willReturn(true);

        missionRegistrationUnitTypeChecker.checkUnitsCanDoMission(List.of(ou), user, mission, MissionType.EXPLORE);

        verify(unitTypeBo, times(1)).canDoMission(user, mission.getTargetPlanet(), List.of(unitTypeEntity), MissionType.EXPLORE);
    }
}
