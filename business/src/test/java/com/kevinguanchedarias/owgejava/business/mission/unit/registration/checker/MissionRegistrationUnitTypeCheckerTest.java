package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MissionRegistrationUnitTypeChecker.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(UnitTypeBo.class)
class MissionRegistrationUnitTypeCheckerTest {
    private final MissionRegistrationUnitTypeChecker missionRegistrationUnitTypeChecker;
    private final UnitTypeBo unitTypeBo;

    @Autowired
    MissionRegistrationUnitTypeCheckerTest(MissionRegistrationUnitTypeChecker missionRegistrationUnitTypeChecker, UnitTypeBo unitTypeBo) {
        this.missionRegistrationUnitTypeChecker = missionRegistrationUnitTypeChecker;
        this.unitTypeBo = unitTypeBo;
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
    }

    @Test
    void checkUnitCanDoMission_should_not_throw() {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        var user = ou.getUser();
        var unitTypeEntity = ou.getUnit().getType();
        given(unitTypeBo.canDoMission(user, mission.getTargetPlanet(), List.of(unitTypeEntity), MissionType.EXPLORE)).willReturn(true);

        missionRegistrationUnitTypeChecker.checkUnitsCanDoMission(List.of(ou), user, mission, MissionType.EXPLORE);

        verify(unitTypeBo, times(1)).canDoMission(user, mission.getTargetPlanet(), List.of(unitTypeEntity), MissionType.EXPLORE);
    }
}
