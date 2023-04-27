package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.fake.FakeUserDeleteListener;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.test.answer.NotReturningAnswer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = UserDeleteService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UserStorageRepository.class,
        SocketIoService.class,
        UserDeleteListener.class,
        FakeUserDeleteListener.class
})
class UserDeleteServiceTest {
    private final UserDeleteService userDeleteService;
    private final UserStorageRepository userStorageRepository;
    private final SocketIoService socketIoService;
    private final UserDeleteListener userDeleteListener;
    private final FakeUserDeleteListener fakeUserDeleteListener;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public UserDeleteServiceTest(
            UserDeleteService userDeleteService,
            UserStorageRepository userStorageRepository,
            SocketIoService socketIoService,
            @Qualifier("com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener#0") UserDeleteListener userDeleteListener,
            FakeUserDeleteListener fakeUserDeleteListener
    ) {
        this.userDeleteService = userDeleteService;
        this.userStorageRepository = userStorageRepository;
        this.socketIoService = socketIoService;
        this.userDeleteListener = userDeleteListener;
        this.fakeUserDeleteListener = fakeUserDeleteListener;
    }

    @Test
    void deleteAccount_should_work() {
        var user = givenUser1();
        var hasFirstInvocationPassed = new AtomicBoolean();
        given(userDeleteListener.order()).willReturn(0);
        given(fakeUserDeleteListener.order()).willReturn(2);
        doAnswer(new NotReturningAnswer(() -> hasFirstInvocationPassed.set(true))).when(userDeleteListener).doDeleteUser(user);
        doAnswer(new NotReturningAnswer(() -> makeTestFailIfBooleanFalse(hasFirstInvocationPassed))).when(fakeUserDeleteListener).doDeleteUser(user);

        userDeleteService.deleteAccount(user);

        verify(userStorageRepository, times(1)).delete(user);
        verify(socketIoService, times(1)).sendOneTimeMessage(eq(USER_ID_1), eq(UserDeleteService.ACCOUNT_DELETED), any(), eq(null));
    }

    private void makeTestFailIfBooleanFalse(AtomicBoolean atomicBoolean) {
        if (!atomicBoolean.get()) {
            throw new IllegalStateException("Should has been set first by the order value");
        }
    }
}
