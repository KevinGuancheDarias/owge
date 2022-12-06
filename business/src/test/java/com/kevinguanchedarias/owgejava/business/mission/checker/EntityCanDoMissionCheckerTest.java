package com.kevinguanchedarias.owgejava.business.mission.checker;

import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenUnitType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = EntityCanDoMissionChecker.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(PlanetRepository.class)
class EntityCanDoMissionCheckerTest {
    private final EntityCanDoMissionChecker entityCanDoMissionChecker;
    private final PlanetRepository planetRepository;

    @Autowired
    public EntityCanDoMissionCheckerTest(EntityCanDoMissionChecker entityCanDoMissionChecker, PlanetRepository planetRepository) {
        this.entityCanDoMissionChecker = entityCanDoMissionChecker;
        this.planetRepository = planetRepository;
    }

    @ParameterizedTest
    @MethodSource("canDoMission_should_work_arguments")
    void canDoMission_should_work(
            MissionSupportEnum missionSupportEnum,
            boolean expectedResult,
            boolean planetIsOfUserProperty,
            int isOfUserPropertyInvocations
    ) {
        var user = givenUser1();
        var targetPlanet = givenTargetPlanet();
        var unitType = givenUnitType();
        unitType.setCanExplore(missionSupportEnum);
        var missionType = MissionType.EXPLORE;
        given(planetRepository.isOfUserProperty(user, targetPlanet)).willReturn(planetIsOfUserProperty);

        assertThat(entityCanDoMissionChecker.canDoMission(user, targetPlanet, unitType, missionType)).isEqualTo(expectedResult);
        verify(planetRepository, times(isOfUserPropertyInvocations)).isOfUserProperty(user, targetPlanet);

    }

    @Test
    void canDoMission_should_throw_on_reflection_error() {
        var user = givenUser1();
        var targetPlanet = givenTargetPlanet();
        var unitType = mock(UnitType.class);
        var missionType = MissionType.EXPLORE;
        given(unitType.getCanExplore()).willThrow(new IllegalArgumentException());

        assertThatThrownBy(() ->
                entityCanDoMissionChecker.canDoMission(user, targetPlanet, unitType, missionType)
        )
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageEndingWith("maybe it is not supported mission");
    }

    @Test
    void canDoMission_should_return_false_on_null_support_enum_value() {
        var user = givenUser1();
        var targetPlanet = givenTargetPlanet();
        var unitType = mock(UnitType.class);
        var missionType = MissionType.EXPLORE;

        assertThat(
                entityCanDoMissionChecker.canDoMission(user, targetPlanet, unitType, missionType)
        ).isFalse();
        verify(planetRepository, never()).isOfUserProperty(any(UserStorage.class), any());
    }

    private static Stream<Arguments> canDoMission_should_work_arguments() {
        return Stream.of(
                Arguments.of(MissionSupportEnum.ANY, true, false, 0),
                Arguments.of(MissionSupportEnum.OWNED_ONLY, true, true, 1),
                Arguments.of(MissionSupportEnum.OWNED_ONLY, false, false, 1),
                Arguments.of(MissionSupportEnum.NONE, false, true, 0)
        );
    }
}
