package com.kevinguanchedarias.owgejava.business.mission.unit.registration;


import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationCanDeployChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationCanStoreUnitChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationPlanetExistsChecker;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.UnitInMap;
import com.kevinguanchedarias.owgejava.pojo.storedunit.StoredUnitWithItsCount;
import com.kevinguanchedarias.owgejava.pojo.storedunit.UnitWithItsStoredUnits;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenDeployedMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.*;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.SOURCE_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMissionMock.*;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MissionRegistrationObtainedUnitLoader.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionRegistrationPlanetExistsChecker.class,
        PlanetRepository.class,
        MissionRegistrationCanDeployChecker.class,
        ObtainedUnitBo.class,
        MissionRegistrationOrphanMissionEraser.class,
        MissionRegistrationCanStoreUnitChecker.class
})
class MissionRegistrationObtainedUnitLoaderTest {
    private final MissionRegistrationObtainedUnitLoader missionRegistrationObtainedUnitLoader;
    private final MissionRegistrationPlanetExistsChecker missionRegistrationPlanetExistsChecker;
    private final PlanetRepository planetRepository;
    private final MissionRegistrationCanDeployChecker missionRegistrationCanDeployChecker;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionRegistrationOrphanMissionEraser missionRegistrationOrphanMissionEraser;
    private final MissionRegistrationCanStoreUnitChecker missionRegistrationCanStoreUnitChecker;

    @Autowired
    MissionRegistrationObtainedUnitLoaderTest(
            MissionRegistrationObtainedUnitLoader missionRegistrationObtainedUnitLoader,
            MissionRegistrationPlanetExistsChecker missionRegistrationPlanetExistsChecker,
            PlanetRepository planetRepository,
            MissionRegistrationCanDeployChecker missionRegistrationCanDeployChecker,
            ObtainedUnitBo obtainedUnitBo,
            MissionRegistrationOrphanMissionEraser missionRegistrationOrphanMissionEraser,
            MissionRegistrationCanStoreUnitChecker missionRegistrationCanStoreUnitChecker) {
        this.missionRegistrationObtainedUnitLoader = missionRegistrationObtainedUnitLoader;
        this.missionRegistrationPlanetExistsChecker = missionRegistrationPlanetExistsChecker;
        this.planetRepository = planetRepository;
        this.missionRegistrationCanDeployChecker = missionRegistrationCanDeployChecker;
        this.obtainedUnitBo = obtainedUnitBo;
        this.missionRegistrationOrphanMissionEraser = missionRegistrationOrphanMissionEraser;
        this.missionRegistrationCanStoreUnitChecker = missionRegistrationCanStoreUnitChecker;
    }

    @Test
    void checkAndLoadObtainedUnits_should_throw_when_involved_units_is_empty() {
        var withNullInvolved = givenUnitMissionInformation(MissionType.EXPLORE).toBuilder().involvedUnits(null).build();
        var withEmptyInvolved = givenUnitMissionInformation(MissionType.EXPLORE).toBuilder().involvedUnits(List.of()).build();
        var message = "involvedUnits can't be empty";

        assertThatThrownBy(() -> missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(withNullInvolved))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessage(message);
        assertThatThrownBy(() -> missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(withEmptyInvolved))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessage(message);

        verify(missionRegistrationPlanetExistsChecker, times(2)).checkPlanetExists(SOURCE_PLANET_ID);
        verify(missionRegistrationPlanetExistsChecker, times(2)).checkPlanetExists(TARGET_PLANET_ID);
    }

    @Test
    void checkAndLoadObtainedUnits_should_throw_when_unit_has_null_count() {
        var information = givenUnitMissionInformation(MissionType.EXPLORE);
        var unitWithNull = information.getInvolvedUnits().get(0).toBuilder().count(null).build();
        var informationWithNullCount = information.toBuilder().involvedUnits(List.of(unitWithNull)).build();

        assertThatThrownBy(() -> missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(informationWithNullCount))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("No count was specified");
    }

    @Test
    void checkAndLoadObtainedUnits_should_throw_on_repeated_unit() {
        var selectedUnit = givenSelectedUnit(null);
        var information = givenUnitMissionInformation(MissionType.EXPLORE, null).toBuilder()
                .involvedUnits(List.of(selectedUnit, selectedUnit))
                .build();
        var ou = givenObtainedUnit1();
        var ouAfterSubtraction = givenObtainedUnit1().toBuilder().id(OBTAINED_UNIT_2_ID).count(4L).build();
        ou.setMission(givenExploreMission());
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(true);
        given(obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(USER_ID_1, UNIT_ID_1, SOURCE_PLANET_ID, null, false))
                .willReturn(ou);
        given(obtainedUnitBo.saveWithSubtraction(ou, SELECTED_UNIT_COUNT, false)).willReturn(ouAfterSubtraction);

        assertThatThrownBy(() -> missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(information))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessage("I18N_ERR_REPEATED_UNIT");
    }

    @Test
    void checkAndLoadObtainedUnits_should_throw_on_stored_unit_weight_overpassed() {
        var storedOverweightUnit = givenSelectedUnit(null).toBuilder().id(UNIT_OVER_WEIGHT_ID).build();
        var selectedUnit = givenSelectedUnit(null).toBuilder().storedUnits(List.of(storedOverweightUnit)).build();
        var information = givenUnitMissionInformation(MissionType.EXPLORE, null).toBuilder()
                .involvedUnits(List.of(selectedUnit))
                .build();
        var ou = givenObtainedUnit1();
        var overweightObtainedUnit = ou.toBuilder().unit(givenOverweightUnit()).id(923727L).build();
        var ouAfterSubtraction = givenObtainedUnit1().toBuilder().id(OBTAINED_UNIT_2_ID).count(4L).build();
        ou.setMission(givenExploreMission());
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(true);
        given(obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(USER_ID_1, UNIT_ID_1, SOURCE_PLANET_ID, null, false))
                .willReturn(ou);
        given(obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(USER_ID_1, UNIT_OVER_WEIGHT_ID, SOURCE_PLANET_ID, null, false))
                .willReturn(overweightObtainedUnit);
        given(obtainedUnitBo.saveWithSubtraction(ou, SELECTED_UNIT_COUNT, false)).willReturn(ouAfterSubtraction);
        given(obtainedUnitBo.saveWithSubtraction(ou, SELECTED_UNIT_COUNT, false)).willReturn(overweightObtainedUnit);

        assertThatThrownBy(() -> missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(information))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessage("I18N_ERR_MAX_WEIGHT_OVERPASSED");
    }

    @ParameterizedTest
    @MethodSource("checkAndLoadObtainedUnits_should_work_arguments")
    void checkAndLoadObtainedUnits_should_work(
            boolean isOfUserProperty,
            Long expirationId,
            ObtainedUnit ouAfterSubtraction,
            Mission unitMission,
            Set<Mission> expectedDeletedMission
    ) {
        var information = givenUnitMissionInformation(MissionType.EXPLORE, expirationId);
        var ou = givenObtainedUnit1();
        ou.setMission(unitMission);
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(isOfUserProperty);
        given(obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(USER_ID_1, UNIT_ID_1, SOURCE_PLANET_ID, expirationId, !isOfUserProperty))
                .willReturn(ou);
        given(obtainedUnitBo.saveWithSubtraction(ou, SELECTED_UNIT_COUNT, false)).willReturn(ouAfterSubtraction);

        var retVal = missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(information);

        verify(missionRegistrationCanDeployChecker, times(1)).checkUnitCanDeploy(ou, information);
        assertThat(retVal).containsEntry(new UnitInMap(UNIT_ID_1, expirationId), new UnitWithItsStoredUnits(ou, List.of()));
        verify(missionRegistrationOrphanMissionEraser, times(1)).doMarkAsDeletedTheOrphanMissions(expectedDeletedMission);
        verify(missionRegistrationCanStoreUnitChecker, never()).checkCanStoreUnit(anyInt(), anyInt());
    }

    @Test
    void checkAndLoadObtainedUnits_should_handle_stored_units() {
        var storedSelectedUnit = givenSelectedUnit(null).toBuilder().id(UNIT_ID_2).count(5L).build();
        var selectedUnit = givenSelectedUnit(null).toBuilder().storedUnits(List.of(storedSelectedUnit)).build();
        var information = givenUnitMissionInformation(MissionType.EXPLORE, null).toBuilder()
                .involvedUnits(List.of(selectedUnit))
                .build();
        var ou = givenObtainedUnit1();
        var storedOu = givenObtainedUnit2().toBuilder().user(ou.getUser()).build();
        var ouAfterSubtraction = ou.toBuilder().id(OBTAINED_UNIT_2_ID + 4).count(4L).build();
        given(planetRepository.isOfUserProperty(USER_ID_1, SOURCE_PLANET_ID)).willReturn(true);
        given(obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(USER_ID_1, UNIT_ID_1, SOURCE_PLANET_ID, null, false))
                .willReturn(ou);
        given(obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(USER_ID_1, UNIT_ID_2, SOURCE_PLANET_ID, null, false))
                .willReturn(storedOu);
        given(obtainedUnitBo.saveWithSubtraction(ou, SELECTED_UNIT_COUNT, false)).willReturn(ouAfterSubtraction);

        var retVal = missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(information);

        verify(missionRegistrationCanDeployChecker, times(1)).checkUnitCanDeploy(ou, information);
        assertThat(retVal).containsEntry(new UnitInMap(UNIT_ID_1, null), new UnitWithItsStoredUnits(ou, List.of(new StoredUnitWithItsCount(storedOu, 5L))));
        verify(obtainedUnitBo, times(1)).saveWithSubtraction(storedOu, 5L, false);
        verify(missionRegistrationCanStoreUnitChecker, times(1)).checkCanStoreUnit(UNIT_ID_1, UNIT_ID_2);
    }

    private static Stream<Arguments> checkAndLoadObtainedUnits_should_work_arguments() {
        var ouId = 44L;
        var count = 99L;
        var deployedMission = givenDeployedMission();
        var ouAfterSubtractionWithoutMission = givenObtainedUnit1().toBuilder().id(ouId).count(count).build();
        var emptyDeletedMissionsList = Set.of();
        return Stream.of(
                Arguments.of(true, null, ouAfterSubtractionWithoutMission, null, emptyDeletedMissionsList),
                Arguments.of(false, null, ouAfterSubtractionWithoutMission, null, emptyDeletedMissionsList),
                Arguments.of(true, 4L, ouAfterSubtractionWithoutMission, null, emptyDeletedMissionsList),
                Arguments.of(true, 4L, ouAfterSubtractionWithoutMission, givenExploreMission(), emptyDeletedMissionsList),
                Arguments.of(true, 4L, null, givenExploreMission(), emptyDeletedMissionsList),
                Arguments.of(true, 4L, null, null, emptyDeletedMissionsList),
                Arguments.of(true, 4L, null, deployedMission, Set.of(deployedMission))
        );
    }
}
