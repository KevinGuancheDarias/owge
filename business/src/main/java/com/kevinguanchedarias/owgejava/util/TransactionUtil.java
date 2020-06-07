package com.kevinguanchedarias.owgejava.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Has methods to interact with Spring transactions
 * 
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class TransactionUtil {

	/**
	 * Run code after the outgoing transaction commits <br>
	 * <b>NOTICE:</b> won't run on rollback
	 *
	 * @param action
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static void doAfterCommit(Runnable action) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				action.run();
			}
		});
	}

	private TransactionUtil() {
		// An util class can't have an instance
	}
}
