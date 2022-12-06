package com.kevinguanchedarias.owgejava.business.planet;

import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlanetCheckerService {
    private final PlanetRepository planetRepository;
    private final UserStorageBo userStorageBo;

    public void checkIsOfUserProperty(UserStorage user, Long planetId) {
        if (!planetRepository.isOfUserProperty(user.getId(), planetId)) {
            throw new SgtBackendInvalidInputException(
                    "Specified planet with id " + planetId + " does NOT belong to the user");
        }
    }

    public void myCheckIsOfUserProperty(Long planetId) {
        checkIsOfUserProperty(userStorageBo.findLoggedIn(), planetId);
    }
}
