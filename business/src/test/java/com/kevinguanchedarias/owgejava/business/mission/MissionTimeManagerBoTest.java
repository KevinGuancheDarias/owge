package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.mock.GalaxyMock;
import com.kevinguanchedarias.owgejava.mock.UnitTypeMock;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroupWithFixed;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MissionTimeManagerBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionConfigurationBo.class,
        ImprovementBo.class,
        ConfigurationBo.class
})
class MissionTimeManagerBoTest {
    private static final int GRACE_TIME_FOR_TEST_RUNNER = 60;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final MissionConfigurationBo missionConfigurationBo;
    private final ImprovementBo improvementBo;
    private final ConfigurationBo configurationBo;

    @Autowired
    MissionTimeManagerBoTest(
            MissionTimeManagerBo missionTimeManagerBo,
            MissionConfigurationBo missionConfigurationBo,
            ImprovementBo improvementBo,
            ConfigurationBo configurationBo
    ) {
        this.missionTimeManagerBo = missionTimeManagerBo;
        this.missionConfigurationBo = missionConfigurationBo;
        this.improvementBo = improvementBo;
        this.configurationBo = configurationBo;
    }

    @Test
    void computeTerminationDate_should_work() {
        double requiredTime = 30;

        var retVal = missionTimeManagerBo.computeTerminationDate(requiredTime);

        var now = LocalDateTime.now(ZoneOffset.UTC);
        assertThat(retVal).isBetween(now.minusSeconds((long) requiredTime), now.plusSeconds((long) requiredTime));
    }

    @Test
    void calculateRequiredTime_should_work() {
        double baseTimeByType = 30;
        given(missionConfigurationBo.findMissionBaseTimeByType(MissionType.EXPLORE)).willReturn((long) baseTimeByType);

        assertThat(missionTimeManagerBo.calculateRequiredTime(MissionType.EXPLORE)).isEqualTo(baseTimeByType);
    }

    @Test
    void handleMissionTimeCalculation_should_do_nothing_if_all_units_has_fixed_speed_impact_group() {
        var ou1 = givenObtainedUnit1();
        var ou2 = givenObtainedUnit2();
        ou1.getUnit().setSpeedImpactGroup(givenSpeedImpactGroupWithFixed());
        ou2.getUnit().setSpeedImpactGroup(givenSpeedImpactGroupWithFixed());
        var mission = givenExploreMission();

        missionTimeManagerBo.handleMissionTimeCalculation(List.of(ou1, ou2), mission, MissionType.EXPLORE);

        assertThat(mission.getRequiredTime()).isNull();
        assertThat(mission.getTerminationDate()).isNull();
        verify(missionConfigurationBo, never()).findMissionBaseTimeByType(MissionType.EXPLORE);
    }

    @Test
    void handleMissionTimeCalculation_should_do_nothing_if_unit_has_null_speed() {
        var ou1 = givenObtainedUnit1();
        ou1.getUnit().setSpeedImpactGroup(givenSpeedImpactGroup());
        ou1.getUnit().setSpeed(null);
        var mission = givenExploreMission();

        missionTimeManagerBo.handleMissionTimeCalculation(List.of(ou1), mission, MissionType.EXPLORE);

        assertThat(mission.getRequiredTime()).isNull();
        assertThat(mission.getTerminationDate()).isNull();
        verify(missionConfigurationBo, never()).findMissionBaseTimeByType(MissionType.EXPLORE);
    }

    @ParameterizedTest
    @MethodSource("handleMissionTimeCalculation_should_work_arguments")
    void handleMissionTimeCalculation_should_work(
            Planet sourcePlanet, Planet targetPlanet, String missionSpeedDivisor, double expectedTotalTime
    ) {
        double slowestSpeed = 19;
        var mission = givenExploreMission();
        mission.setSourcePlanet(sourcePlanet);
        mission.setTargetPlanet(targetPlanet);
        mission.getTargetPlanet().setPlanetNumber(PLANET_NUMBER - 1);
        var user = givenUser1();
        var expectedUnitType = UnitTypeMock.givenUnitType(170);
        long unitTypeSpeedImprovement = 80;
        var leftMultiplier = "4";

        mission.setUser(user);
        var skippedDueToFixed = givenObtainedUnit1();
        skippedDueToFixed.getUnit().setSpeedImpactGroup(givenSpeedImpactGroupWithFixed());
        var skippedDueToNullSpi = givenObtainedUnit1();
        var skippedDueToNullSpeed = givenObtainedUnit1();
        skippedDueToNullSpeed.getUnit().setSpeedImpactGroup(givenSpeedImpactGroup());
        skippedDueToNullSpeed.getUnit().setSpeed(null);
        var skippedDueToNegativeSpeed = givenObtainedUnit1();
        skippedDueToNegativeSpeed.getUnit().setSpeedImpactGroup(givenSpeedImpactGroup());
        skippedDueToNegativeSpeed.getUnit().setSpeed(-1D);
        var validSlowerUnit = givenObtainedUnit1();
        validSlowerUnit.getUnit().setSpeedImpactGroup(givenSpeedImpactGroup());
        validSlowerUnit.getUnit().setSpeed(slowestSpeed);
        validSlowerUnit.getUnit().setType(expectedUnitType);
        var validFasterUnit = givenObtainedUnit1();
        validFasterUnit.getUnit().setSpeedImpactGroup(givenSpeedImpactGroup());
        validFasterUnit.getUnit().setSpeed(40D);
        var validIntermediateSpeed = givenObtainedUnit1();
        validIntermediateSpeed.getUnit().setSpeedImpactGroup(givenSpeedImpactGroup(4));
        validIntermediateSpeed.getUnit().setSpeed(30D);
        var improvementMock = mock(GroupedImprovement.class);
        given(improvementMock.findUnitTypeImprovement(ImprovementTypeEnum.SPEED, expectedUnitType)).willReturn(unitTypeSpeedImprovement);
        given(improvementBo.findUserImprovement(user)).willReturn(improvementMock);
        given(improvementBo.findAsRational((double) unitTypeSpeedImprovement)).willReturn(0.8);
        given(missionConfigurationBo.findMissionBaseTimeByType(MissionType.EXPLORE)).willReturn(60L);
        given(configurationBo.findOrSetDefault("MISSION_SPEED_DIVISOR_EXPLORE", "1"))
                .willReturn(Configuration.builder().value(missionSpeedDivisor).build());
        given(configurationBo.findOrSetDefault("MISSION_SPEED_EXPLORE_SAME_Q", "50"))
                .willReturn(Configuration.builder().value(leftMultiplier).build());
        given(configurationBo.findOrSetDefault("MISSION_SPEED_EXPLORE_DIFF_G", "2000"))
                .willReturn(Configuration.builder().value(leftMultiplier).build());
        given(configurationBo.findOrSetDefault("MISSION_SPEED_EXPLORE_DIFF_S", "200"))
                .willReturn(Configuration.builder().value(leftMultiplier).build());
        given(configurationBo.findOrSetDefault("MISSION_SPEED_EXPLORE_DIFF_Q", "100"))
                .willReturn(Configuration.builder().value(leftMultiplier).build());
        given(configurationBo.findOrSetDefault("MISSION_SPEED_EXPLORE_P_MOVE_COST", "0.01")).willReturn(Configuration.builder().value("0.01").build());
        given(configurationBo.findOrSetDefault("MISSION_SPEED_EXPLORE_Q_MOVE_COST", "0.02")).willReturn(Configuration.builder().value("0.02").build());
        given(configurationBo.findOrSetDefault("MISSION_SPEED_EXPLORE_S_MOVE_COST", "0.03")).willReturn(Configuration.builder().value("0.03").build());
        given(configurationBo.findOrSetDefault("MISSION_SPEED_EXPLORE_G_MOVE_COST", "0.15")).willReturn(Configuration.builder().value("0.15").build());

        missionTimeManagerBo.handleMissionTimeCalculation(
                List.of(skippedDueToFixed, skippedDueToNullSpi, skippedDueToNullSpeed, skippedDueToNegativeSpeed, validFasterUnit, validSlowerUnit, validIntermediateSpeed),
                mission,
                MissionType.EXPLORE
        );

        verify(improvementBo, times(1)).findAsRational((double) unitTypeSpeedImprovement);
        assertThat(mission.getRequiredTime()).isEqualTo(expectedTotalTime);
        var now = LocalDateTime.now(ZoneOffset.UTC);
        assertThat(mission.getTerminationDate()).isBetween(
                now.minusSeconds((long) expectedTotalTime).minusSeconds(GRACE_TIME_FOR_TEST_RUNNER),
                now.plusSeconds((long) expectedTotalTime).plusSeconds(GRACE_TIME_FOR_TEST_RUNNER)
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1,2",
            "4,4"
    }, nullValues = "null")
    void handleCustomDuration_should_work(Long customDuration, Double expectedRequiredTime) {
        var mission = givenExploreMission();
        var now = LocalDateTime.now(ZoneOffset.UTC);
        mission.setRequiredTime(2D);
        mission.setTerminationDate(now.plusSeconds(2));

        missionTimeManagerBo.handleCustomDuration(mission, customDuration);

        assertThat(mission.getRequiredTime()).isEqualTo(expectedRequiredTime);
        assertThat(mission.getTerminationDate()).isBetween(
                now.minusSeconds(expectedRequiredTime.longValue()).minusSeconds(GRACE_TIME_FOR_TEST_RUNNER),
                now.plusSeconds(expectedRequiredTime.longValue()).plusSeconds(GRACE_TIME_FOR_TEST_RUNNER)
        );
    }

    @Test
    void handleCustomDuration_should_do_nothing_if_null() {
        var mission = givenExploreMission();

        missionTimeManagerBo.handleCustomDuration(mission, null);

        assertThat(mission.getRequiredTime()).isNull();
        assertThat(mission.getTerminationDate()).isNull();
    }

    private static Stream<Arguments> handleMissionTimeCalculation_should_work_arguments() {
        var sourcePlanet = givenSourcePlanet();
        var differentGalaxyTarget = givenTargetPlanet().toBuilder().galaxy(GalaxyMock.givenGalaxy(28)).build();
        var differentSectorTarget = givenTargetPlanet().toBuilder().sector(PLANET_SECTOR - 2).build();
        var differentQuadrantTarget = givenTargetPlanet().toBuilder().quadrant(PLANET_QUADRANT - 4).build();
        return Stream.of(
                Arguments.of(sourcePlanet, givenTargetPlanet(), "2", 61.31599997058511),
                Arguments.of(sourcePlanet, differentGalaxyTarget, "0", 102.11200298070906),
                Arguments.of(sourcePlanet, differentSectorTarget, "2", 69.21200003921986),
                Arguments.of(sourcePlanet, differentQuadrantTarget, "2", 71.84399949014187)
        );
    }
}
