package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.UnitInMap;
import com.kevinguanchedarias.owgejava.pojo.storedunit.UnitWithItsStoredUnits;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenGatherMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenPlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.UnitMissionMock.SELECTED_UNIT_COUNT;
import static com.kevinguanchedarias.owgejava.mock.UnitMissionMock.givenUnitMissionInformation;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = MissionRegistrationUnitManager.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(UnitRepository.class)
class MissionRegistrationUnitManagerTest {
    private final MissionRegistrationUnitManager missionRegistrationUnitManager;
    private final UnitRepository unitRepository;

    @Autowired
    MissionRegistrationUnitManagerTest(
            MissionRegistrationUnitManager missionRegistrationUnitManager,
            UnitRepository unitRepository
    ) {
        this.missionRegistrationUnitManager = missionRegistrationUnitManager;
        this.unitRepository = unitRepository;
    }

    @ParameterizedTest
    @MethodSource("manageUnitsRegistration_should_work_arguments")
    void manageUnitsRegistration_should_work(
            boolean isEnemyPlanet,
            int expectedSizeAlteredMissions,
            Mission firstDeploymentMission,
            Planet expectedSourcePlanet
    ) {
        var expirationId = 99282441L;
        var information = givenUnitMissionInformation(MissionType.EXPLORE, expirationId);
        var mission = givenGatherMission();
        var user = givenUser1();
        var ouDb = givenObtainedUnit1().toBuilder()
                .firstDeploymentMission(firstDeploymentMission)
                .expirationId(expirationId)
                .build();
        var dbUnits = Map.of(new UnitInMap(UNIT_ID_1, expirationId), new UnitWithItsStoredUnits(ouDb, null));
        var unit = givenUnit1();
        given(unitRepository.getReferenceById(UNIT_ID_1)).willReturn(unit);

        var retVal = missionRegistrationUnitManager.manageUnitsRegistration(information, dbUnits, isEnemyPlanet, user, mission);

        assertThat(retVal.getAlteredVisibilityMissions()).hasSize(expectedSizeAlteredMissions);
        assertThat(retVal.getUnits()).isNotEmpty();
        var savedUnit = retVal.getUnits().get(0);
        assertThat(savedUnit.getFirstDeploymentMission()).isEqualTo(firstDeploymentMission);
        assertThat(savedUnit.getCount()).isEqualTo(SELECTED_UNIT_COUNT);
        assertThat(savedUnit.getUser()).isEqualTo(user);
        assertThat(savedUnit.getUnit()).isEqualTo(unit);
        assertThat(savedUnit.getExpirationId()).isEqualTo(expirationId);
        assertThat(savedUnit.getSourcePlanet()).isEqualTo(expectedSourcePlanet);
        assertThat(savedUnit.getTargetPlanet()).isEqualTo(mission.getTargetPlanet());
    }

    private static Stream<Arguments> manageUnitsRegistration_should_work_arguments() {
        var firstDeploymentMission = givenExploreMission();
        var firstDeploySourcePlanet = givenPlanet(19L);
        firstDeploymentMission.setSourcePlanet(firstDeploySourcePlanet);
        var regularSourcePlanet = givenSourcePlanet();
        return Stream.of(
                Arguments.of(true, 1, firstDeploymentMission, firstDeploySourcePlanet),
                Arguments.of(false, 0, null, regularSourcePlanet)
        );
    }
}
