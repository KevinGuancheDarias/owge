package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.AuthenticationBo;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.TokenUserMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = UserSessionService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UserStorageRepository.class,
        AuthenticationBo.class
})
class UserSessionServiceTest {
    private final UserSessionService userSessionService;
    private final AuthenticationBo authenticationBo;
    private final UserStorageRepository repository;

    @Autowired
    UserSessionServiceTest(UserSessionService userSessionService, AuthenticationBo authenticationBo, UserStorageRepository repository) {
        this.userSessionService = userSessionService;
        this.authenticationBo = authenticationBo;
        this.repository = repository;
    }

    @BeforeEach
    public void setup_logged_in() {
        given(authenticationBo.findTokenUser()).willReturn(givenTokenUser());
    }


    @Test
    void findLoggedIn_should_handle_null_token() {
        reset(authenticationBo);

        assertThat(userSessionService.findLoggedIn()).isNull();
    }

    @Test
    void findLoggedInWithReference_should_work() {
        var user = givenUser1();
        given(repository.getReferenceById(TOKEN_USER_ID)).willReturn(user);

        var retVal = userSessionService.findLoggedInWithReference();

        assertThat(retVal).isSameAs(user);
    }

    @ParameterizedTest
    @CsvSource({
            TOKEN_USER_EMAIL + ",fooUser,1",
            "foo@foo.foo," + TOKEN_USERNAME + ",1",
            TOKEN_USER_EMAIL + ',' + TOKEN_USERNAME + ",0"
    })
    void findLoggedInWithDetails_should_work_and_save_user_if_changed_from_token(String email, String username, int timesSave) {
        var user = givenUser1();
        user.setEmail(email);
        user.setUsername(username);
        given(repository.findById(USER_ID_1)).willReturn(Optional.of(user));

        var savedUser = userSessionService.findLoggedInWithDetails();

        verify(repository, times(timesSave)).save(user);
        assertThat(savedUser.getEmail()).isEqualTo(TOKEN_USER_EMAIL);
        assertThat(savedUser.getUsername()).isEqualTo(TOKEN_USERNAME);
    }

    @Test
    void findLoggedInWithDetails_should_handle_null_token_result() {
        reset(authenticationBo);

        assertThat(userSessionService.findLoggedInWithDetails()).isNull();
    }
}
