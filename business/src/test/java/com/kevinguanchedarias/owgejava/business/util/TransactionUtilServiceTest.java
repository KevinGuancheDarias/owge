package com.kevinguanchedarias.owgejava.business.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    void doAfterCommit_should_work() {
        try (var transactionSynchronizationManagerMock = mockStatic(TransactionSynchronizationManager.class)) {
            var actionMock = mock(Runnable.class);
            transactionSynchronizationManagerMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(invocationOnMock -> {
                        invocationOnMock.getArgument(0, TransactionSynchronization.class).afterCommit();
                        return null;
                    });

            transactionUtilService.doAfterCommit(actionMock);

            transactionSynchronizationManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
            verify(actionMock, times(1)).run();
        }
    }
}
