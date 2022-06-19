package com.kevinguanchedarias.owgejava.business;

import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.6
 */
@Service
public class AsyncRunnerBo {
    private static final Logger LOG = Logger.getLogger(AsyncRunnerBo.class);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     */
    @Async("contextAwareTaskExecutor")
    public <T, R> CompletableFuture<R> runAssync(T param, Function<T, R> supplier) {
        return CompletableFuture.completedFuture(supplier.apply(param));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.10
     */
    public void runAssyncWithoutContext(Runnable supplier) {
        new Thread(supplier).start();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.10
     */
    public void runAssyncWithoutContextDelayed(Runnable task, long delay) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                LOG.error("dA FUCK?", e);
                Thread.currentThread().interrupt();
            }
            task.run();
        });
        thread.start();
    }

    public void runAssyncWithoutContextDelayed(Runnable task) {
        runAssyncWithoutContextDelayed(task, 200);
    }

}
