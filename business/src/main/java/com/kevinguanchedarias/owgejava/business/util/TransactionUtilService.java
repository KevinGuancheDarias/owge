package com.kevinguanchedarias.owgejava.business.util;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
public class TransactionUtilService {

	/**
	 * Runs lambda using a transaction with propagation REQUIRED
	 *
	 * @param action
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void runWithRequired(Runnable action) {
		action.run();
	}
}
