package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.mysql.MysqlLockUtilService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializes per-user operations that follow a check-then-act pattern (e.g. "register a mission only
 * if the user has no equivalent one running"). Mirrors {@link com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService}
 * but keyed by user id instead of planet id.
 */
@Service
@AllArgsConstructor
public class UserLockUtilService {
    public static final String USER_LOCK_KEY_PREFIX = "user_lock_";

    private final MysqlLockUtilService mysqlLockUtilService;

    public void doInsideLockById(List<Integer> userIds, Runnable runnable) {
        mysqlLockUtilService.doInsideLock(
                userIds.stream().map(this::mapUserToLockKey).collect(Collectors.toUnmodifiableSet()),
                runnable
        );
    }

    private String mapUserToLockKey(int userId) {
        return USER_LOCK_KEY_PREFIX + userId;
    }
}
