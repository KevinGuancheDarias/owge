package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTypeBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class MissionRegistrationCanDeployChecker {
    private final ConfigurationBo configurationBo;
    private final MissionTypeBo missionTypeBo;
    private final PlanetRepository planetRepository;

    /**
     * Checks if the DEPLOY mission is allowed
     *
     * @throws SgtBackendInvalidInputException If the deployment mission is
     *                                         <b>globally</b> disabled
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    public void checkDeployedAllowed(MissionType missionType) {
        if (missionType == MissionType.DEPLOY
                && configurationBo.findDeployMissionConfiguration().equals(DeployMissionConfigurationEnum.DISALLOWED)) {
            throw new SgtBackendInvalidInputException("The deployment mission is globally disabled");
        }
    }

    /**
     * Checks if the current obtained unit can do deploy (if already deployed in
     * some cases, cannot)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    public void checkUnitCanDeploy(ObtainedUnit currentObtainedUnit, UnitMissionInformation missionInformation) {
        var unitMissionType = missionTypeBo.resolve(currentObtainedUnit.getMission());
        boolean isOfUserProperty = planetRepository.isOfUserProperty(missionInformation.getUserId(),
                missionInformation.getTargetPlanetId());
        var deployConfiguration = configurationBo.findDeployMissionConfiguration();
        var isOnlyOnce = deployConfiguration == DeployMissionConfigurationEnum.ONLY_ONCE_RETURN_SOURCE
                || deployConfiguration == DeployMissionConfigurationEnum.ONLY_ONCE_RETURN_DEPLOYED;
        if (isOnlyOnce && !isOfUserProperty
                && unitMissionType == MissionType.DEPLOYED
                && missionInformation.getMissionType() == MissionType.DEPLOY
        ) {
            throw new SgtBackendInvalidInputException("You can't do a deploy mission after a deploy mission");
        }
    }
}
