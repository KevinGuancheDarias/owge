package com.kevinguanchedarias.owgejava.business;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @since 0.9.6
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
@Async("contextAwareTaskExecutor")
public class AsyncRunnerBo {

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
	public <T, R> CompletableFuture<R> runAssync(T param, Function<T, R> supplier) {
		return CompletableFuture.completedFuture(supplier.apply(param));
	}

}
