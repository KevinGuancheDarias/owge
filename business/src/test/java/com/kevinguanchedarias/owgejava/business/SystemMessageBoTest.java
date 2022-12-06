package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.SystemMessage;
import com.kevinguanchedarias.owgejava.entity.UserReadSystemMessage;
import com.kevinguanchedarias.owgejava.pojo.SystemMessageUser;
import com.kevinguanchedarias.owgejava.repository.SystemMessageRepository;
import com.kevinguanchedarias.owgejava.repository.UserReadSystemMessageRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.EntityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.business.SystemMessageBo.SYSTEM_MESSAGE_CHANGE;
import static com.kevinguanchedarias.owgejava.mock.SystemMessageMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = SystemMessageBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        SystemMessageRepository.class,
        TransactionUtilService.class,
        SocketIoService.class,
        UserStorageBo.class,
        UserReadSystemMessageRepository.class
})
class SystemMessageBoTest {

    private final SystemMessageBo systemMessageBo;
    private final transient SystemMessageRepository repository;
    private final transient TransactionUtilService transactionUtilService;
    private final transient SocketIoService socketIoService;
    private final UserStorageBo userStorageBo;
    private final transient UserReadSystemMessageRepository userReadRepository;

    private InvokeSupplierLambdaAnswer<List<SystemMessageUser>> emitChangesSocketAnswer;

    @Autowired
    SystemMessageBoTest(
            SystemMessageBo systemMessageBo,
            SystemMessageRepository repository,
            TransactionUtilService transactionUtilService,
            SocketIoService socketIoService,
            UserStorageBo userStorageBo,
            UserReadSystemMessageRepository userReadRepository
    ) {
        this.systemMessageBo = systemMessageBo;
        this.repository = repository;
        this.transactionUtilService = transactionUtilService;
        this.socketIoService = socketIoService;
        this.userStorageBo = userStorageBo;
        this.userReadRepository = userReadRepository;
    }

    @BeforeEach
    void setup_emit_changes_test() {
        given(repository.findAll()).willReturn(List.of(givenSystemMessage()));
        given(userStorageBo.findAllIds()).willReturn(List.of(USER_ID_1));
        emitChangesSocketAnswer = new InvokeSupplierLambdaAnswer<>(2);
        doAnswer(emitChangesSocketAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(SYSTEM_MESSAGE_CHANGE), any());
    }

    @Test
    void deleteOld_should_work() {
        var systemMessage = givenSystemMessage();
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).runWithRequired(any());
        given(repository.findByCreationDateLessThan(any())).willReturn(List.of(systemMessage));

        systemMessageBo.deleteOld();

        verify(repository, times(1)).deleteAll(List.of(systemMessage));
    }

    @Test
    void deleteOld_should_do_nothing_if_empty_result() {
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).runWithRequired(any());

        systemMessageBo.deleteOld();

        verify(repository, never()).deleteAll(any());
    }

    @Test
    void save_should_work() {
        var messageDto = givenSystemMessageDto();
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());
        try (var mockedStatic = mockStatic(EntityUtil.class)) {
            systemMessageBo.save(messageDto);

            var captor = ArgumentCaptor.forClass(SystemMessage.class);
            verify(repository, times(1)).save(captor.capture());
            var saved = captor.getValue();
            mockedStatic.verify(() -> EntityUtil.requireNullId(saved), times(1));
            verifyEmitChange();
        }
    }

    @Test
    void markAsRead_should_work() {
        var user = givenUser1();
        var message = givenSystemMessage();
        given(repository.findAllById(List.of(SYSTEM_MESSAGE_ID))).willReturn(List.of(message));
        doAnswer(new InvokeRunnableLambdaAnswer(0)).when(transactionUtilService).doAfterCommit(any());

        systemMessageBo.markAsRead(List.of(SYSTEM_MESSAGE_ID), user);

        var captor = ArgumentCaptor.forClass(UserReadSystemMessage.class);
        verify(userReadRepository, times(1)).save(captor.capture());
        var savedUserRead = captor.getValue();
        assertThat(savedUserRead.getUser()).isEqualTo(user);
        assertThat(savedUserRead.getMessage()).isEqualTo(message);
        verifyEmitChange();
    }

    private void verifyEmitChange() {
        var sentData = emitChangesSocketAnswer.getResult();
        assertThat(sentData).hasSize(1);
        var message = sentData.get(0);
        assertThat(message.getId()).isEqualTo(SYSTEM_MESSAGE_ID);
        assertThat(message.getContent()).isEqualTo(SYSTEM_MESSAGE_CONTENT);
        assertThat(message.getCreationDate()).isNotNull();
        assertThat(message.isRead()).isFalse();
    }
}
