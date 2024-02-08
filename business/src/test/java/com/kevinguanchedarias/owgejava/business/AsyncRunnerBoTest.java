package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.util.ThreadUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class AsyncRunnerBoTest {
    private AsyncRunnerBo asyncRunnerBo;

    @BeforeEach
    void setup() {
        asyncRunnerBo = new AsyncRunnerBo();
    }

    @SuppressWarnings("unchecked")
    @Test
    void runAsync_should_work() {
        try (var mockedStatic = mockStatic(CompletableFuture.class)) {
            var appliedResult = "bar";
            var functionMock = mock(Function.class);
            var fakedFuture = mock(CompletableFuture.class);
            var param = "foo";
            given(functionMock.apply(param)).willReturn(appliedResult);
            mockedStatic.when(() -> CompletableFuture.completedFuture(appliedResult)).thenReturn(fakedFuture);

            var result = asyncRunnerBo.runAsync(param, functionMock);

            assertThat(result).isSameAs(fakedFuture);
            verify(functionMock, times(1)).apply(param);
        }
    }

    @Test
    void runAsyncWithoutContext_should_work() {
        var runnableMock = mock(Runnable.class);
        var atomicReference = new AtomicReference<Runnable>();
        try (var mockedConstruction = mockConstruction(Thread.class,
                (mock, context) -> atomicReference.set((Runnable) context.arguments().get(0)))
        ) {

            asyncRunnerBo.runAsyncWithoutContext(runnableMock);

            var threadInstance = mockedConstruction.constructed().get(0);

            assertThat(atomicReference.get()).isSameAs(runnableMock);
            verify(threadInstance, times(1)).start();
        }
    }

    @Test
    void runAsyncWithoutContextDelayed_should_work() {
        var runnableMock = mock(Runnable.class);
        var atomicReference = new AtomicReference<Runnable>();
        try (
                var mockedConstruction = mockConstruction(Thread.class,
                        (mock, context) -> atomicReference.set((Runnable) context.arguments().get(0)));
                var mockedStatic = mockStatic(ThreadUtil.class)
        ) {

            asyncRunnerBo.runAsyncWithoutContextDelayed(runnableMock);

            var threadInstance = mockedConstruction.constructed().get(0);
            var threadBody = atomicReference.get();
            verify(runnableMock, never()).run();
            mockedStatic.verify(() -> ThreadUtil.sleep(200), never());
            verify(threadInstance, times(1)).setPriority(Thread.NORM_PRIORITY - 1);
            verify(threadInstance, times(1)).start();

            // Run actual body
            threadBody.run();

            verify(runnableMock, times(1)).run();
            mockedStatic.verify(() -> ThreadUtil.sleep(200), times(1));
        }
    }
}
