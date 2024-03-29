package com.kevinguanchedarias.owgejava.business.planet;

import com.kevinguanchedarias.owgejava.business.mysql.MysqlLockUtilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Set;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = PlanetLockUtilService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(MysqlLockUtilService.class)
class PlanetLockUtilServiceTest {
    private final PlanetLockUtilService planetLockUtilService;
    private final MysqlLockUtilService mysqlLockUtilService;

    private Runnable runnableMock;

    @Autowired
    PlanetLockUtilServiceTest(PlanetLockUtilService planetLockUtilService, MysqlLockUtilService mysqlLockUtilService) {
        this.planetLockUtilService = planetLockUtilService;
        this.mysqlLockUtilService = mysqlLockUtilService;
    }

    @BeforeEach
    public void setup() {
        runnableMock = mock(Runnable.class);
    }

    @Test
    void doInsideLock_should_work() {
        var planets = List.of(givenSourcePlanet(), givenTargetPlanet());

        planetLockUtilService.doInsideLock(planets, runnableMock);

        verify(mysqlLockUtilService, times(1)).doInsideLock(
                Set.of(expectedLockKey(SOURCE_PLANET_ID), expectedLockKey(TARGET_PLANET_ID)),
                runnableMock
        );
    }

    @Test
    void doInsideLockById_should_work() {
        planetLockUtilService.doInsideLockById(List.of(SOURCE_PLANET_ID, TARGET_PLANET_ID), runnableMock);

        verify(mysqlLockUtilService, times(1)).doInsideLock(
                Set.of(expectedLockKey(SOURCE_PLANET_ID), expectedLockKey(TARGET_PLANET_ID)),
                runnableMock
        );
    }

    private String expectedLockKey(long planetId) {
        return PlanetLockUtilService.PLANET_LOCK_KEY_PREFIX + planetId;
    }
}
