package com.kevinguanchedarias.owgejava.business.mission.unit.registration;


import com.kevinguanchedarias.owgejava.business.mission.MissionTimeManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTypeBo;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionTypeMock.givenMissinType;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.UnitMissionMock.givenUnitMissionInformation;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = MissionRegistrationPreparer.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UserStorageRepository.class,
        MissionTimeManagerBo.class,
        PlanetRepository.class,
        MissionTypeBo.class
})
class MissionRegistrationPreparerTest {
    private final MissionRegistrationPreparer missionRegistrationPreparer;
    private final UserStorageRepository userStorageRepository;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final PlanetRepository planetRepository;
    private final MissionTypeBo missionTypeBo;

    @Autowired
    MissionRegistrationPreparerTest(
            MissionRegistrationPreparer missionRegistrationPreparer,
            UserStorageRepository userStorageRepository,
            MissionTimeManagerBo missionTimeManagerBo,
            PlanetRepository planetRepository,
            MissionTypeBo missionTypeBo
    ) {
        this.missionRegistrationPreparer = missionRegistrationPreparer;
        this.userStorageRepository = userStorageRepository;
        this.missionTimeManagerBo = missionTimeManagerBo;
        this.planetRepository = planetRepository;
        this.missionTypeBo = missionTypeBo;
    }

    @ParameterizedTest
    @MethodSource("prepareMission_should_work_arguments")
    void prepareMission_should_work(
            Long sourcePlanetId,
            Planet sourcePlanet,
            Long targetPlanetId,
            Planet targetPlanet
    ) {
        var missionType = MissionType.EXPLORE;
        var missionTypeEntity = givenMissinType(missionType);
        double requiredTime = 19;
        var terminationDate = LocalDateTime.now();
        var information = givenUnitMissionInformation(missionType).toBuilder()
                .sourcePlanetId(sourcePlanetId)
                .targetPlanetId(targetPlanetId)
                .build();
        var user = givenUser1();
        given(missionTimeManagerBo.calculateRequiredTime(missionType)).willReturn(requiredTime);
        given(missionTypeBo.find(missionType)).willReturn(missionTypeEntity);
        given(userStorageRepository.getById(USER_ID_1)).willReturn(user);
        given(planetRepository.getById(sourcePlanetId)).willReturn(sourcePlanet);
        given(planetRepository.getById(targetPlanetId)).willReturn(targetPlanet);
        given(missionTimeManagerBo.computeTerminationDate(requiredTime)).willReturn(terminationDate);

        var retVal = missionRegistrationPreparer.prepareMission(information, missionType);

        assertThat(retVal.getStartingDate()).isNotNull();
        assertThat(retVal.getType()).isEqualTo(missionTypeEntity);
        assertThat(retVal.getUser()).isEqualTo(user);
        assertThat(retVal.getRequiredTime()).isEqualTo(requiredTime);
        assertThat(retVal.getSourcePlanet()).isEqualTo(sourcePlanet);
        assertThat(retVal.getTargetPlanet()).isEqualTo(targetPlanet);
        assertThat(retVal.getTerminationDate()).isEqualTo(terminationDate);
    }

    private static Stream<Arguments> prepareMission_should_work_arguments() {
        return Stream.of(
                Arguments.of(null, null, null, null),
                Arguments.of(1L, givenSourcePlanet(), null, null),
                Arguments.of(null, null, 2L, givenTargetPlanet())
        );
    }
}
