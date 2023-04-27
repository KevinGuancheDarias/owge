package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.repository.WebsocketEventsInformationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = WebsocketEventsInformationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        WebsocketEventsInformationRepository.class,
        UserStorageRepository.class
})
class WebsocketEventsInformationBoTest {
    private final WebsocketEventsInformationBo websocketEventsInformationBo;
    private final WebsocketEventsInformationRepository repository;
    private final UserStorageRepository userStorageRepository;

    @Autowired
    WebsocketEventsInformationBoTest
            (WebsocketEventsInformationBo websocketEventsInformationBo,
             WebsocketEventsInformationRepository repository,
             UserStorageRepository userStorageRepository
            ) {
        this.websocketEventsInformationBo = websocketEventsInformationBo;
        this.repository = repository;
        this.userStorageRepository = userStorageRepository;
    }

    @Test
    void clear_should_work() {
        given(userStorageRepository.findAllIds()).willReturn(List.of(USER_ID_1));

        websocketEventsInformationBo.clear();

        verify(repository, times(1)).updateLastSent(eq(USER_ID_1), any());
    }


    @Test
    void order_should_return_zero() {
        assertThat(websocketEventsInformationBo.order()).isZero();
    }

    @Test
    void doDeleteUser_should_work() {
        var user = givenUser1();

        websocketEventsInformationBo.doDeleteUser(user);

        verify(repository, times(1)).deleteByEventNameUserIdUserId(USER_ID_1);
    }
}
