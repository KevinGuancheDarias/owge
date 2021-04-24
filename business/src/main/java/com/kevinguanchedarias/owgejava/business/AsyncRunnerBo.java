package com.kevinguanchedarias.owgejava.business;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @since 0.9.6
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
public class AsyncRunnerBo {
	private static final Logger LOG = Logger.getLogger(AsyncRunnerBo.class);

	/**
	 *
	 * @param <T>
	 * @param <R>
	 * @param param
	 * @param supplier
	 * @return
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Async("contextAwareTaskExecutor")
	public <T, R> CompletableFuture<R> runAssync(T param, Function<T, R> supplier) {
		return CompletableFuture.completedFuture(supplier.apply(param));
	}

	/**
	 *
	 * @param <T>
	 * @param <R>
	 * @param param
	 * @param supplier
	 * @return
	 * @since 0.9.10
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void runAssyncWithoutContext(Runnable supplier) {
		new Thread(supplier).start();
	}

	/**
	 *
	 * @param task
	 * @since 0.9.10
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
