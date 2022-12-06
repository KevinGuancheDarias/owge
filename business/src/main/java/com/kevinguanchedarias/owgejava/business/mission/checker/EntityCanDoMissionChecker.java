package com.kevinguanchedarias.owgejava.business.mission.checker;

import com.kevinguanchedarias.owgejava.entity.EntityWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service
@AllArgsConstructor
public class EntityCanDoMissionChecker {

    private final PlanetRepository planetRepository;

    /**
     * Test if the given entity with mission limitations can do the mission
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public boolean canDoMission(UserStorage user, Planet targetPlanet,
                                EntityWithMissionLimitation<Integer> entityWithMissionLimitation, MissionType missionType) {
        String targetMethod = "getCan" + WordUtils.capitalizeFully(missionType.name(), '_').replace("_", "");
        try {
            MissionSupportEnum missionSupport = ((MissionSupportEnum) entityWithMissionLimitation.getClass()
                    .getMethod(targetMethod).invoke(entityWithMissionLimitation));
            return switch (missionSupport == null ? MissionSupportEnum.NONE : missionSupport) {
                case ANY -> true;
                case OWNED_ONLY -> planetRepository.isOfUserProperty(user, targetPlanet);
                default -> false;
            };
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                 | SecurityException e) {
            throw new SgtBackendInvalidInputException(
                    "Could not invoke method " + targetMethod + " maybe it is not supported mission", e);
        }
    }
}
