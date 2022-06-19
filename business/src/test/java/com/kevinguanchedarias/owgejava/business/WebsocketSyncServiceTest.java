package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructWebsocketSyncService;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        classes = NonPostConstructWebsocketSyncService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UserStorageBo.class,
        SyncSource.class,
        WebsocketEventsInformationBo.class
})
class WebsocketSyncServiceTest {
    private final NonPostConstructWebsocketSyncService websocketSyncService;
    private final UserStorageBo userStorageBo;
    private final SyncSource syncSource;
    private final WebsocketEventsInformationBo websocketEventsInformationBo;

    @Autowired
    public WebsocketSyncServiceTest(
            NonPostConstructWebsocketSyncService websocketSyncService,
            UserStorageBo userStorageBo,
            SyncSource syncSource,
            WebsocketEventsInformationBo websocketEventsInformationBo
    ) {
        this.websocketSyncService = websocketSyncService;
        this.userStorageBo = userStorageBo;
        this.syncSource = syncSource;
        this.websocketEventsInformationBo = websocketEventsInformationBo;
    }

    @SuppressWarnings("unchecked")
    @Test
    void init_should_work() {
        var mapMock = mock(Map.class);
        ReflectionTestUtils.setField(websocketSyncService, "handlers", mapMock);
        Function<UserStorage, Object> handlerFunction = user -> null;
        given(syncSource.findSyncHandlers()).willReturn(Map.of("foo", handlerFunction));

        websocketSyncService.invokeRealInit();

        verify(mapMock, times(1)).put("foo", handlerFunction);
    }

    @Test
    void init_should_throw_when_duplicated_handler_key() {
        var mapMock = mock(Map.class);
        ReflectionTestUtils.setField(websocketSyncService, "handlers", mapMock);
        Function<UserStorage, Object> handlerFunction = user -> null;
        given(syncSource.findSyncHandlers()).willReturn(Map.of("foo", handlerFunction));
        given(mapMock.containsKey("foo")).willReturn(true);

        assertThatThrownBy(websocketSyncService::invokeRealInit)
                .isInstanceOf(ProgrammingException.class);
    }

    @Test
    void init_should_warn_when_no_sync_sources(CapturedOutput capturedOutput) {
        var fullyNullInstance = new WebsocketSyncService(null, null, null);
        fullyNullInstance.init();

        assertThat(capturedOutput.getOut()).contains("No sync sources");
    }

    @SuppressWarnings("unchecked")
    @Test
    void findWantedData_should_work() {
        var mapMock = mock(Map.class);
        var existingHandlerKey = "power";
        var nonExistingHandlerKey = "noob";
        var functionMock = mock(Function.class);
        var data = "Hello World";
        ReflectionTestUtils.setField(websocketSyncService, "handlers", mapMock);
        var user = givenUser1();
        given(userStorageBo.findLoggedIn()).willReturn(user);
        given(mapMock.containsKey(existingHandlerKey)).willReturn(true);
        given(mapMock.get(existingHandlerKey)).willReturn(functionMock);
        given(functionMock.apply(user)).willReturn(data);

        var result = websocketSyncService.findWantedData(List.of(existingHandlerKey, nonExistingHandlerKey));

        verify(mapMock, times(1)).get(existingHandlerKey);
        verify(mapMock, never()).get(nonExistingHandlerKey);
        verify(websocketEventsInformationBo, times(1)).save(eq(existingHandlerKey), eq(USER_ID_1), notNull());
        assertThat(result)
                .isNotNull()
                .containsKey(existingHandlerKey);
        var content = (Map<String, String>) result.get(existingHandlerKey);
        assertThat(content)
                .containsEntry("data", data)
                .containsKey("lastSent");

    }
}
