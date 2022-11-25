package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MissionRegistrationPlanetExistsChecker {
    private final PlanetRepository planetRepository;

    /**
     * Checks if the input planet exists
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void checkPlanetExists(Long planetId) {
        if (!planetRepository.existsById(planetId)) {
            throw new PlanetNotFoundException("No such planet with id " + planetId);
        }
    }
}
