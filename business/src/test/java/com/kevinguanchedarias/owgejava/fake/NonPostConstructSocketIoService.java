package com.kevinguanchedarias.owgejava.fake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@Service
@Primary
public class NonPostConstructSocketIoService extends SocketIoService {
    private static final ObjectMapper OBJECT_MAPPER_MOCK = mock(ObjectMapper.class);

    static {
        given(OBJECT_MAPPER_MOCK.copy()).willReturn(mock(ObjectMapper.class));
    }

    public NonPostConstructSocketIoService() {
        super(OBJECT_MAPPER_MOCK);
        // Replace the async executor with a direct (synchronous) executor for tests
        sendExecutor = new AbstractExecutorService() {
            private boolean shutdown = false;

            @Override
            public void execute(Runnable command) {
                command.run();
            }

            @Override
            public void shutdown() {
                shutdown = true;
            }

            @Override
            public List<Runnable> shutdownNow() {
                shutdown = true;
                return List.of();
            }

            @Override
            public boolean isShutdown() {
                return shutdown;
            }

            @Override
            public boolean isTerminated() {
                return shutdown;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                return true;
            }
        };
    }

    @Override
    public void init() {
        // Don't invoke init
    }

    @Override
    public void onContextReady() {
        // Don't invoke onContextReady
    }

    public void realInit() {
        super.init();
    }

    public void realContextReady() {
        super.onContextReady();
    }

    @Override
    public void destroy() {
        // Don't invoke
    }

    public void realDestroy() {
        super.destroy();
    }
}
