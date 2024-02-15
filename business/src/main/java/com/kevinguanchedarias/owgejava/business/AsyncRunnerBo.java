package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.util.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.6
 */
@Service
@Slf4j
public class AsyncRunnerBo {

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     */
    @Async("contextAwareTaskExecutor")
    public <T, R> CompletableFuture<R> runAsync(T param, Function<T, R> supplier) {
        return CompletableFuture.completedFuture(supplier.apply(param));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.10
     */
    public void runAsyncWithoutContext(Runnable supplier) {
        new Thread(supplier).start();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.10
     */
    public void runAsyncWithoutContextDelayed(Runnable task, long delay, int priority) {
        var thread = ThreadUtil.ofVirtualUnStarted(() -> {
            ThreadUtil.sleep(delay);
            task.run();
        });
        thread.setPriority(priority);
        thread.start();
    }

    public void runAsyncWithoutContextDelayed(Runnable task, long delay) {
        runAsyncWithoutContextDelayed(task, delay, Thread.NORM_PRIORITY - 1);
    }

    public void runAsyncWithoutContextDelayed(Runnable task) {
        runAsyncWithoutContextDelayed(task, 200);
    }

}
