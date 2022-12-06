package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = MissionRegistrationUserExistsChecker.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(UserStorageRepository.class)
class MissionRegistrationUserExistsCheckerTest {
    private final MissionRegistrationUserExistsChecker missionRegistrationUserExistsChecker;
    private final UserStorageRepository userStorageRepository;

    @Autowired
    MissionRegistrationUserExistsCheckerTest(
            MissionRegistrationUserExistsChecker missionRegistrationUserExistsChecker,
            UserStorageRepository userStorageRepository
    ) {
        this.missionRegistrationUserExistsChecker = missionRegistrationUserExistsChecker;
        this.userStorageRepository = userStorageRepository;
    }

    @Test
    void checkUserExists_should_throw() {
        assertThatThrownBy(() -> missionRegistrationUserExistsChecker.checkUserExists(USER_ID_1))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void checkUserExists_should_work() {
        given(userStorageRepository.existsById(USER_ID_1)).willReturn(true);

        missionRegistrationUserExistsChecker.checkUserExists(USER_ID_1);

        verify(userStorageRepository, times(1)).existsById(USER_ID_1);
    }
}
