package com.kevinguanchedarias.owgejava.business.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = TransactionUtilService.class
)
class TransactionUtilServiceTest {
    private final TransactionUtilService transactionUtilService;

    @Autowired
    TransactionUtilServiceTest(TransactionUtilService transactionUtilService) {
        this.transactionUtilService = transactionUtilService;
    }

    @Test
    @SneakyThrows
    void runWithRequiresNew_should_work() {
        var annotation = AnnotationUtils.findAnnotation(
                TransactionUtilService.class.getMethod("runWithRequiresNew", Runnable.class), Transactional.class
        );

        assert annotation != null;
        assertThat(annotation.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        var runnableMock = mock(Runnable.class);

        transactionUtilService.runWithRequiresNew(runnableMock);

        verify(runnableMock, times(1)).run();
    }

    @ParameterizedTest
    @CsvSource({
            "true,0",
            "false,1"
    })
    void doAfterCommit_should_work(boolean testResourceBinding, int timesBindResource) {
        var actionMock = mock(Runnable.class);
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            if (testResourceBinding) {
                mockedStatic.when(() -> TransactionSynchronizationManager.hasResource(any()))
                        .thenReturn(false)
                        .thenReturn(true);
            } else {
                mockedStatic.when(() -> TransactionSynchronizationManager.hasResource(any()))
                        .thenReturn(false);
            }
            mockedStatic.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(invocationOnMock -> {
                        invocationOnMock.getArgument(0, TransactionSynchronization.class).afterCommit();
                        return null;
                    });

            transactionUtilService.doAfterCommit(actionMock);

            mockedStatic.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
            mockedStatic.verify(() -> TransactionSynchronizationManager.bindResource(any(), eq(true)), times(timesBindResource));
            verify(actionMock, times(1)).run();
        }
    }

    /**
     * This is because else the method would never run, as it's adding an after commit event handler
     * to a transaction that already triggered that event (so the list of handlers is already been iterated)
     */
    @Test
    void doAfterCommit_should_just_run_te_runnable_if_already_in_committing_state() {
        var actionMock = mock(Runnable.class);
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(() -> TransactionSynchronizationManager.hasResource(any())).thenReturn(true);

            transactionUtilService.doAfterCommit(actionMock);

            mockedStatic.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()), never());
        }
        verify(actionMock, times(1)).run();
    }

    @Test
    void clearStatus() {
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            transactionUtilService.clearStatus();

            mockedStatic.verify(() -> TransactionSynchronizationManager.unbindResourceIfPossible(anyString()), times(1));
        }
    }
}
