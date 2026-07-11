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

    @Test
    void doAfterCommit_should_register_synchronization_and_bind_unbind_resource() {
        var actionMock = mock(Runnable.class);
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(() -> TransactionSynchronizationManager.hasResource(any())).thenReturn(false);
            mockedStatic.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            mockedStatic.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(invocationOnMock -> {
                        invocationOnMock.getArgument(0, TransactionSynchronization.class).afterCommit();
                        return null;
                    });

            transactionUtilService.doAfterCommit(actionMock);

            mockedStatic.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
            mockedStatic.verify(() -> TransactionSynchronizationManager.bindResource(any(), eq(true)), times(1));
            mockedStatic.verify(() -> TransactionSynchronizationManager.unbindResourceIfPossible(any()), times(1));
            verify(actionMock, times(1)).run();
        }
    }

    @Test
    void doAfterCommit_should_run_immediately_when_no_synchronization_active() {
        var actionMock = mock(Runnable.class);
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(() -> TransactionSynchronizationManager.hasResource(any())).thenReturn(false);
            mockedStatic.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);

            transactionUtilService.doAfterCommit(actionMock);

            mockedStatic.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()), never());
            verify(actionMock, times(1)).run();
        }
    }

    @Test
    void doAfterCommit_resource_unbound_after_commit_so_new_sync_registers_normally() {
        // After a transaction commits and its afterCommit action ran, the ALREADY_COMMITTING_KEY
        // must be unbound so a subsequent doAfterCommit in a NEW active synchronization registers.
        var firstAction = mock(Runnable.class);
        var secondAction = mock(Runnable.class);
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            // First call: no resource, sync active — registers and fires afterCommit which binds then unbinds
            mockedStatic.when(() -> TransactionSynchronizationManager.hasResource(any())).thenReturn(false);
            mockedStatic.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            mockedStatic.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(invocationOnMock -> {
                        invocationOnMock.getArgument(0, TransactionSynchronization.class).afterCommit();
                        return null;
                    });

            transactionUtilService.doAfterCommit(firstAction);
            // At this point the finally block has run unbindResourceIfPossible, so hasResource is false again.
            // Second call: still no resource, sync active — must register (not run inline)
            transactionUtilService.doAfterCommit(secondAction);

            mockedStatic.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()), times(2));
            verify(firstAction, times(1)).run();
            verify(secondAction, times(1)).run();
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
    void doAfterCommit_nested_call_during_afterCommit_runs_inline() {
        // A doAfterCommit called from within an afterCommit callback must run inline.
        var innerAction = mock(Runnable.class);
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            mockedStatic.when(() -> TransactionSynchronizationManager.hasResource(any())).thenReturn(false);
            mockedStatic.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            mockedStatic.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(invocationOnMock -> {
                        // Simulate Spring calling afterCommit: resource is already bound at this point
                        mockedStatic.when(() -> TransactionSynchronizationManager.hasResource(any())).thenReturn(true);
                        invocationOnMock.getArgument(0, TransactionSynchronization.class).afterCommit();
                        return null;
                    });

            // Outer call registers normally; during its afterCommit it calls doAfterCommit again (nested)
            transactionUtilService.doAfterCommit(() -> transactionUtilService.doAfterCommit(innerAction));

            // Inner action ran inline (not via registerSynchronization a second time)
            mockedStatic.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()), times(1));
            verify(innerAction, times(1)).run();
        }
    }

    @Test
    void clearStatus() {
        try (var mockedStatic = mockStatic(TransactionSynchronizationManager.class)) {
            transactionUtilService.clearStatus();

            mockedStatic.verify(() -> TransactionSynchronizationManager.unbindResourceIfPossible(anyString()), times(1));
        }
    }
}
