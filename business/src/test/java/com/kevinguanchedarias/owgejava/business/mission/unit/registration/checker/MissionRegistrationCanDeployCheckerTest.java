package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTypeBo;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.mock.UnitMissionMock;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MissionRegistrationCanDeployChecker.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ConfigurationBo.class,
        MissionTypeBo.class,
        PlanetRepository.class
})
class MissionRegistrationCanDeployCheckerTest {
    private final MissionRegistrationCanDeployChecker missionRegistrationCanDeployChecker;
    private final ConfigurationBo configurationBo;
    private final MissionTypeBo missionTypeBo;
    private final PlanetRepository planetRepository;

    @Autowired
    MissionRegistrationCanDeployCheckerTest(
            MissionRegistrationCanDeployChecker missionRegistrationCanDeployChecker,
            ConfigurationBo configurationBo,
            MissionTypeBo missionTypeBo,
            PlanetRepository planetRepository
    ) {
        this.missionRegistrationCanDeployChecker = missionRegistrationCanDeployChecker;
        this.configurationBo = configurationBo;
        this.missionTypeBo = missionTypeBo;
        this.planetRepository = planetRepository;
    }

    @ParameterizedTest
    @CsvSource({
            "DEPLOY,DISALLOWED",
    })
    void checkDeployedAllowed_should_throw(MissionType missionType, DeployMissionConfigurationEnum deployMissionConfigurationEnum) {
        given(configurationBo.findDeployMissionConfiguration()).willReturn(deployMissionConfigurationEnum);

        assertThatThrownBy(() -> missionRegistrationCanDeployChecker.checkDeployedAllowed(missionType))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("The deployment mission");
    }

    @ParameterizedTest
    @CsvSource({
            "EXPLORE,DISALLOWED,0",
            "DEPLOY,FREEDOM,1"
    })
    void checkDeployedAllowed_should_not_throw(
            MissionType missionType, DeployMissionConfigurationEnum deployMissionConfigurationEnum, int timesFindDeployMissionConfiguration
    ) {
        given(configurationBo.findDeployMissionConfiguration()).willReturn(deployMissionConfigurationEnum);

        missionRegistrationCanDeployChecker.checkDeployedAllowed(missionType);

        verify(configurationBo, times(timesFindDeployMissionConfiguration)).findDeployMissionConfiguration();
    }

    @ParameterizedTest
    @CsvSource({
            "false,ONLY_ONCE_RETURN_SOURCE,DEPLOYED,DEPLOY",
            "false,ONLY_ONCE_RETURN_DEPLOYED,DEPLOYED,DEPLOY",
    })
    void checkUnitCanDeploy_should_throw(
            boolean isOfUserProperty,
            DeployMissionConfigurationEnum deployConfiguration,
            MissionType currentUnitMissionType,
            MissionType newMissionType
    ) {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        var information = UnitMissionMock.givenUnitMissionInformation(newMissionType);
        ou.setMission(mission);
        given(missionTypeBo.resolve(mission)).willReturn(currentUnitMissionType);
        given(planetRepository.isOfUserProperty(USER_ID_1, TARGET_PLANET_ID)).willReturn(isOfUserProperty);
        given(configurationBo.findDeployMissionConfiguration()).willReturn(deployConfiguration);

        assertThatThrownBy(() -> this.missionRegistrationCanDeployChecker.checkUnitCanDeploy(ou, information))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("after a deploy mission");
    }

    @ParameterizedTest
    @CsvSource({
            "false,FREEDOM,DEPLOYED,DEPLOY",
            "true,ONLY_ONCE_RETURN_SOURCE,DEPLOYED,DEPLOY",
            "false,ONLY_ONCE_RETURN_SOURCE,EXPLORE,DEPLOY",
            "false,ONLY_ONCE_RETURN_SOURCE,DEPLOYED,EXPLORE"
    })
    void checkUnitCanDeploy_should_not_throw(
            boolean isOfUserProperty,
            DeployMissionConfigurationEnum deployConfiguration,
            MissionType currentUnitMissionType,
            MissionType newMissionType
    ) {
        var mission = givenExploreMission();
        var ou = givenObtainedUnit1();
        var information = UnitMissionMock.givenUnitMissionInformation(newMissionType);
        ou.setMission(mission);
        given(missionTypeBo.resolve(mission)).willReturn(currentUnitMissionType);
        given(planetRepository.isOfUserProperty(USER_ID_1, TARGET_PLANET_ID)).willReturn(isOfUserProperty);
        given(configurationBo.findDeployMissionConfiguration()).willReturn(deployConfiguration);

        missionRegistrationCanDeployChecker.checkUnitCanDeploy(ou, information);

        verify(configurationBo, times(1)).findDeployMissionConfiguration();
    }
}
