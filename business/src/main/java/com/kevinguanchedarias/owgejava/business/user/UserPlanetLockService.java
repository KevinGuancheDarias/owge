package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Locks user planets
 */
@Service
@RequiredArgsConstructor
public class UserPlanetLockService {
    private final PlanetRepository planetRepository;
    private final PlanetLockUtilService planetLockUtilService;
    private final AsyncRunnerBo asyncRunnerBo;

    @Resource
    @Lazy
    private UserPlanetLockService selfProxied;

    @Transactional
    public void runLockedForUser(UserStorage user, Runnable task) {
        planetLockUtilService.doInsideLock(planetRepository.findByOwnerId(user.getId()), task);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void runLockedForUserDelayed(UserStorage user, Runnable task, long delay) {
        asyncRunnerBo.runAsyncWithoutContextDelayed(() ->
                        selfProxied.runLockedForUser(user, task)
                , delay);
    }
}
