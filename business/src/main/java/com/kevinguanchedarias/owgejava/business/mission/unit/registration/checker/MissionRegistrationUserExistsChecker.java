package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MissionRegistrationUserExistsChecker {
    private final UserStorageRepository userStorageRepository;

    /**
     * Checks if the user exists (in this universe), throws if not
     *
     * @throws UserNotFoundException If user doesn't exists
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void checkUserExists(Integer userId) {
        if (!userStorageRepository.existsById(userId)) {
            throw new UserNotFoundException("No user with id " + userId);
        }
    }
}
