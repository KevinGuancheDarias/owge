package com.kevinguanchedarias.owgejava.business;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.owgejava.configurations.WebsocketConfiguration;
import com.kevinguanchedarias.owgejava.dto.WebsocketEventsInformationDto;
import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructSocketIoService;
import com.kevinguanchedarias.owgejava.filter.OwgeJwtAuthenticationFilter;
import com.kevinguanchedarias.owgejava.pojo.WebsocketMessage;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.kevinguanchedarias.owgejava.mock.AuditMock.AUDIT_IP;
import static com.kevinguanchedarias.owgejava.mock.ConfigurationMock.givenConfiguration;
import static com.kevinguanchedarias.owgejava.mock.TokenUserMock.TOKEN_USER_ID;
import static com.kevinguanchedarias.owgejava.mock.TokenUserMock.givenTokenUser;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = NonPostConstructSocketIoService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        WebsocketConfiguration.class,
        WebsocketEventsInformationBo.class,
        ConfigurationBo.class,
        UserStorageRepository.class,
        AsyncRunnerBo.class,
        OwgeJwtAuthenticationFilter.class,
        ObjectMapper.class
})
@ExtendWith(OutputCaptureExtension.class)
class SocketIoServiceTest {
    private static final String AUTHENTICATION_DATA = "FOO_BAR";
    private static final String AUTHENTICATION_TOKEN = "ey64foo";

    private final NonPostConstructSocketIoService socketIoService;
    private final OwgeJwtAuthenticationFilter owgeJwtAuthenticationFilter;
    private final WebsocketEventsInformationBo websocketEventsInformationBo;
    private final ConfigurationBo configurationBo;
    private final AsyncRunnerBo asyncRunnerBo;
    private final UserStorageRepository userStorageRepository;
    private final WebsocketConfiguration websocketConfiguration;


    @Autowired
    SocketIoServiceTest(
            NonPostConstructSocketIoService socketIoService,
            OwgeJwtAuthenticationFilter owgeJwtAuthenticationFilter,
            WebsocketEventsInformationBo websocketEventsInformationBo,
            ConfigurationBo configurationBo,
            AsyncRunnerBo asyncRunnerBo,
            UserStorageRepository userStorageRepository,
            WebsocketConfiguration websocketConfiguration) {
        this.socketIoService = socketIoService;
        this.owgeJwtAuthenticationFilter = owgeJwtAuthenticationFilter;
        this.websocketEventsInformationBo = websocketEventsInformationBo;
        this.configurationBo = configurationBo;
        this.asyncRunnerBo = asyncRunnerBo;
        this.userStorageRepository = userStorageRepository;
        this.websocketConfiguration = websocketConfiguration;
    }

    @Test
    void init_should_work() {
        try (var mockedConstructor = mockConstruction(SocketIOServer.class)) {
            socketIoService.realInit();

            var serverMock = mockedConstructor.constructed().get(0);

            verify(serverMock, times(1)).addConnectListener(any());
            verify(serverMock, times(1)).addEventListener(eq(SocketIoService.AUTHENTICATION), eq(String.class), any());
        }
    }

    @Test
    void connectListener_should_work(CapturedOutput capturedOutput) {
        try (var mockedConstructor = mockConstruction(SocketIOServer.class)) {
            socketIoService.realInit();

            var serverMock = mockedConstructor.constructed().get(0);

            var captor = ArgumentCaptor.forClass(ConnectListener.class);
            verify(serverMock, times(1)).addConnectListener(captor.capture());
            var listener = captor.getValue();
            var clientMock = mock(SocketIOClient.class);
            var socketAddressMock = mock(SocketAddress.class);
            given(socketAddressMock.toString()).willReturn(AUDIT_IP);
            given(clientMock.getRemoteAddress()).willReturn(socketAddressMock);
            listener.onConnect(clientMock);

            assertThat(capturedOutput.getOut())
                    .contains("Client connected from " + AUDIT_IP);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticationListener_should_send_error_when_empty_token(CapturedOutput capturedOutput) throws Exception {
        try (var mockedConstructor = mockConstruction(SocketIOServer.class)) {
            socketIoService.realInit();
            var serverMock = mockedConstructor.constructed().get(0);
            var clientMock = mock(SocketIOClient.class);
            var captor = ArgumentCaptor.forClass(DataListener.class);
            verify(serverMock, times(1)).addEventListener(eq(SocketIoService.AUTHENTICATION), eq(String.class), captor.capture());
            var listener = captor.getValue();
            given(socketIoService.mapper.readValue(eq(AUTHENTICATION_DATA), any(TypeReference.class))).willReturn(Map.of("value", ""));
            var expectedErrorText = "invalid token sent from client";

            listener.onData(clientMock, AUTHENTICATION_DATA, null);

            assertThat(capturedOutput.getOut()).contains(expectedErrorText);
            var messageCaptor = ArgumentCaptor.forClass(WebsocketMessage.class);
            verify(clientMock, times(1)).sendEvent(eq(SocketIoService.AUTHENTICATION), messageCaptor.capture());
            var message = messageCaptor.getValue();
            assertThat(message.getEventName()).isEqualTo(SocketIoService.AUTHENTICATION);
            assertThat(message.getStatus()).isEqualTo("error");
            assertThat(message.getValue()).isEqualTo(expectedErrorText);
            verify(clientMock, times(1)).disconnect();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticationListener_should_send_error_when_authentication_failed(CapturedOutput capturedOutput) throws Exception {
        try (var mockedConstructor = mockConstruction(SocketIOServer.class)) {
            socketIoService.realInit();
            var serverMock = mockedConstructor.constructed().get(0);
            var clientMock = mock(SocketIOClient.class);
            var captor = ArgumentCaptor.forClass(DataListener.class);
            verify(serverMock, times(1)).addEventListener(eq(SocketIoService.AUTHENTICATION), eq(String.class), captor.capture());
            var listener = captor.getValue();
            given(socketIoService.mapper.readValue(eq(AUTHENTICATION_DATA), any(TypeReference.class)))
                    .willReturn(Map.of("value", AUTHENTICATION_TOKEN));
            var expectedErrorText = "Invalid credentials";

            listener.onData(clientMock, AUTHENTICATION_DATA, null);

            verify(owgeJwtAuthenticationFilter, times(1)).findUserFromToken(AUTHENTICATION_TOKEN);
            assertThat(capturedOutput.getOut()).contains(expectedErrorText);
            var messageCaptor = ArgumentCaptor.forClass(WebsocketMessage.class);
            verify(clientMock, times(1)).sendEvent(eq(SocketIoService.AUTHENTICATION), messageCaptor.capture());
            var message = messageCaptor.getValue();
            assertThat(message.getEventName()).isEqualTo(SocketIoService.AUTHENTICATION);
            assertThat(message.getStatus()).isEqualTo("error");
            assertThat(message.getValue()).isEqualTo(expectedErrorText);
            verify(clientMock, times(1)).disconnect();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticationListener_should_send_correct_authentication_on_success() throws Exception {
        try (var mockedConstructor = mockConstruction(SocketIOServer.class)) {
            socketIoService.realInit();
            var serverMock = mockedConstructor.constructed().get(0);
            var clientMock = mock(SocketIOClient.class);
            var captor = ArgumentCaptor.forClass(DataListener.class);
            verify(serverMock, times(1)).addEventListener(eq(SocketIoService.AUTHENTICATION), eq(String.class), captor.capture());
            var listener = captor.getValue();
            var tokenUser = givenTokenUser();
            given(socketIoService.mapper.readValue(eq(AUTHENTICATION_DATA), any(TypeReference.class)))
                    .willReturn(Map.of("value", AUTHENTICATION_TOKEN));
            given(owgeJwtAuthenticationFilter.findUserFromToken(AUTHENTICATION_TOKEN)).willReturn(tokenUser);
            var eventInfo = new WebsocketEventsInformationDto();
            eventInfo.setEventName(SocketIoService.AUTHENTICATION);
            List<WebsocketEventsInformationDto> eventInfoList = new ArrayList<>();
            eventInfoList.add(eventInfo);
            given(websocketEventsInformationBo.findByUserId(TOKEN_USER_ID)).willReturn(List.of());
            given(websocketEventsInformationBo.toDto(anyList())).willReturn(eventInfoList);
            given(configurationBo.findConfigurationParam("UNIVERSE_ID")).willReturn(givenConfiguration("1"));

            listener.onData(clientMock, AUTHENTICATION_DATA, null);

            verify(owgeJwtAuthenticationFilter, times(1)).findUserFromToken(AUTHENTICATION_TOKEN);
            verify(clientMock, times(1)).set(SocketIoService.USER_TOKEN_KEY, tokenUser);
            var messageCaptor = ArgumentCaptor.forClass(WebsocketMessage.class);
            verify(clientMock, times(1)).sendEvent(eq(SocketIoService.AUTHENTICATION), messageCaptor.capture());
            var message = messageCaptor.getValue();
            assertThat(message.getEventName()).isEqualTo(SocketIoService.AUTHENTICATION);
            assertThat(message.getStatus()).isEqualTo("ok");
            assertThat(message.getValue()).isEqualTo(eventInfoList);
            var universeInfo = ((List<WebsocketEventsInformationDto>) message.getValue()).get(1);
            assertThat(universeInfo.getEventName()).isEqualTo("_universe_id:1");
        }
    }

    @Test
    void onContextReady_should_work(CapturedOutput capturedOutput) {
        var host = "foo.com";
        var port = 7474;
        var server = mock(SocketIOServer.class);
        given(websocketConfiguration.getHostname()).willReturn(host);
        given(websocketConfiguration.getPort()).willReturn(port);
        socketIoService.server = server;

        socketIoService.realContextReady();

        assertThat(capturedOutput.getOut()).contains("Starting websocket at ws://foo.com:7474");
        verify(server, times(1)).start();
    }

    @Test
    void destroy_should_work(CapturedOutput capturedOutput) {
        var server = mock(SocketIOServer.class);
        socketIoService.server = server;

        socketIoService.realDestroy();

        assertThat(capturedOutput.getOut()).contains("Closing websocket");
        verify(server, times(1)).stop();
    }

    @Test
    void sendMessage_should_work_for_specific_user() {
        var user = givenUser1();
        var content = "HelloWorld";
        var server = mock(SocketIOServer.class);
        var validClientMock = mock(SocketIOClient.class);
        var invalidClientMock = mock(SocketIOClient.class);
        var eventName = "HELLO";
        given(server.getAllClients()).willReturn(List.of(invalidClientMock, validClientMock));
        given(validClientMock.get(SocketIoService.USER_TOKEN_KEY)).willReturn(givenTokenUser());
        given(websocketEventsInformationBo.save(any(WebsocketEventsInformation.class))).will(returnsFirstArg());
        socketIoService.server = server;
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(asyncRunnerBo).runAsyncWithoutContext(any());

        socketIoService.sendMessage(user, eventName, () -> content);

        var saveCaptor = ArgumentCaptor.forClass(WebsocketEventsInformation.class);
        verify(websocketEventsInformationBo, times(1)).save(saveCaptor.capture());
        var saved = saveCaptor.getValue();
        assertThat(saved.getEventNameUserId().getEventName()).isEqualTo(eventName);
        assertThat(saved.getEventNameUserId().getUserId()).isEqualTo(TOKEN_USER_ID);
        var sentMessageCaptor = ArgumentCaptor.forClass(WebsocketMessage.class);
        verify(validClientMock, times(1)).sendEvent(eq("deliver_message"), sentMessageCaptor.capture());
        var sentMessage = sentMessageCaptor.getValue();
        assertThat(sentMessage.getEventName()).isEqualTo(eventName);
        assertThat(sentMessage.getValue()).isEqualTo(content);
    }

    @Test
    void sendMessage_should_work_for_all_users() {
        var user = givenUser1();
        var content = "HelloWorld";
        var server = mock(SocketIOServer.class);
        var clientMock = mock(SocketIOClient.class);
        var eventName = "HELLO";
        given(server.getAllClients()).willReturn(List.of(clientMock));
        given(clientMock.get(SocketIoService.USER_TOKEN_KEY)).willReturn(givenTokenUser());
        given(userStorageRepository.findAll()).willReturn(List.of(user));
        socketIoService.server = server;
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(asyncRunnerBo).runAsyncWithoutContext(any());

        socketIoService.sendMessage(0, eventName, () -> content);

        var saveCaptor = ArgumentCaptor.forClass(WebsocketEventsInformation.class);
        verify(websocketEventsInformationBo, times(1)).save(saveCaptor.capture());
        var saved = saveCaptor.getValue();
        assertThat(saved.getEventNameUserId().getEventName()).isEqualTo(eventName);
        assertThat(saved.getEventNameUserId().getUserId()).isEqualTo(TOKEN_USER_ID);
        var sentMessageCaptor = ArgumentCaptor.forClass(WebsocketMessage.class);
        verify(clientMock, times(1)).sendEvent(eq("deliver_message"), sentMessageCaptor.capture());
        var sentMessage = sentMessageCaptor.getValue();
        assertThat(sentMessage.getEventName()).isEqualTo(eventName);
        assertThat(sentMessage.getValue()).isEqualTo(content);
    }

    @Test
    void sendMessage_should_work_for_all_users_and_log_transaction_active_warning(CapturedOutput capturedOutput) {
        var user = givenUser1();
        var content = "HelloWorld";
        var server = mock(SocketIOServer.class);
        var clientMock = mock(SocketIOClient.class);
        var eventName = "HELLO";
        given(server.getAllClients()).willReturn(List.of(clientMock));
        given(clientMock.get(SocketIoService.USER_TOKEN_KEY)).willReturn(givenTokenUser());
        given(userStorageRepository.findAll()).willReturn(List.of(user));
        socketIoService.server = server;
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(asyncRunnerBo).runAsyncWithoutContext(any());

        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

            socketIoService.sendMessage(0, eventName, () -> content);

            var saveCaptor = ArgumentCaptor.forClass(WebsocketEventsInformation.class);
            verify(websocketEventsInformationBo, times(1)).save(saveCaptor.capture());
            var saved = saveCaptor.getValue();
            assertThat(saved.getEventNameUserId().getEventName()).isEqualTo(eventName);
            assertThat(saved.getEventNameUserId().getUserId()).isEqualTo(TOKEN_USER_ID);
            var sentMessageCaptor = ArgumentCaptor.forClass(WebsocketMessage.class);
            verify(clientMock, times(1)).sendEvent(eq("deliver_message"), sentMessageCaptor.capture());
            var sentMessage = sentMessageCaptor.getValue();
            assertThat(sentMessage.getEventName()).isEqualTo(eventName);
            assertThat(sentMessage.getValue()).isEqualTo(content);
            assertThat(capturedOutput.getOut()).contains("if everything is nice");
        }
    }

    @Test
    void sendMessage_should_run_not_connected_action_on_empty_users() {
        var user = givenUser1();
        var content = "HelloWorld";
        var server = mock(SocketIOServer.class);
        var eventName = "HELLO";
        given(server.getAllClients()).willReturn(List.of());
        given(userStorageRepository.findAll()).willReturn(List.of(user));
        socketIoService.server = server;
        var notConnectedActionMock = mock(Runnable.class);

        socketIoService.sendMessage(0, eventName, () -> content, notConnectedActionMock);

        verify(notConnectedActionMock, times(1)).run();
        verifyNoInteractions(asyncRunnerBo);
    }

    @Test
    void sendMessage_should_do_nothing_on_empty_users_and_null_not_connected_action() {
        var server = mock(SocketIOServer.class);
        given(server.getAllClients()).willReturn(List.of());
        socketIoService.server = server;

        socketIoService.sendMessage(givenUser1(), "FOO", () -> "BAR", null);

        verifyNoInteractions(asyncRunnerBo);
    }

    @Test
    void clearCache_should_work() {
        var server = mock(SocketIOServer.class);
        socketIoService.server = server;
        var clientMock = mock(SocketIOClient.class);
        given(server.getAllClients()).willReturn(List.of(clientMock));

        socketIoService.clearCache();

        verify(websocketEventsInformationBo, times(1)).clear();
        verify(clientMock).sendEvent("cache_clear", "null");
    }
}
