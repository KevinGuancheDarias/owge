package com.kevinguanchedarias.owgejava.business.planet;

import com.kevinguanchedarias.owgejava.business.mysql.MysqlLockUtilService;
import com.kevinguanchedarias.owgejava.entity.Planet;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PlanetLockUtilService {
    public static final String PLANET_LOCK_KEY_PREFIX = "planet_lock_";

    private final MysqlLockUtilService mysqlLockUtilService;

    public void doInsideLock(List<Planet> planets, Runnable runnable) {
        doInsideLockById(planets.stream().map(Planet::getId).toList(), runnable);
    }

    public void doInsideLockById(List<Long> planetIds, Runnable runnable) {
        mysqlLockUtilService.doInsideLock(
                planetIds.stream().map(this::mapPlanetToLockKey).collect(Collectors.toUnmodifiableSet()),
                runnable
        );
    }

    private String mapPlanetToLockKey(long planetId) {
        return PLANET_LOCK_KEY_PREFIX + planetId;
    }
}
