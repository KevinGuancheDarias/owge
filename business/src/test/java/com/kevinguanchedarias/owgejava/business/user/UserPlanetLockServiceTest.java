package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenSourcePlanet;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = UserPlanetLockService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        PlanetRepository.class,
        PlanetLockUtilService.class,
        AsyncRunnerBo.class,
})
@AllArgsConstructor(onConstructor_ = @Autowired)
class UserPlanetLockServiceTest {
    private final UserPlanetLockService userPlanetLockService;
    private final PlanetRepository planetRepository;
    private final PlanetLockUtilService planetLockUtilService;
    private final AsyncRunnerBo asyncRunnerBo;

    @Test
    void runLockedForUser_should_work() {
        var planetList = List.of(givenSourcePlanet());
        var runnableMock = mock(Runnable.class);
        given(planetRepository.findByOwnerId(USER_ID_1)).willReturn(planetList);

        userPlanetLockService.runLockedForUser(givenUser1(), runnableMock);

        verify(planetLockUtilService, times(1)).doInsideLock(planetList, runnableMock);
    }

    @Test
    void runLockedForUserDelayed_should_work() {
        var planetList = List.of(givenSourcePlanet());
        var runnableMock = mock(Runnable.class);
        long delay = 218;
        given(planetRepository.findByOwnerId(USER_ID_1)).willReturn(planetList);
        doAnswer(new InvokeRunnableLambdaAnswer(0))
                .when(asyncRunnerBo)
                .runAsyncWithoutContextDelayed(AdditionalMatchers.not(eq(runnableMock)), eq(delay));

        userPlanetLockService.runLockedForUserDelayed(givenUser1(), runnableMock, delay);

        verify(planetLockUtilService, times(1)).doInsideLock(planetList, runnableMock);
    }
}
