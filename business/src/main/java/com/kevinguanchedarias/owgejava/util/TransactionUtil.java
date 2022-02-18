package com.kevinguanchedarias.owgejava.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Has methods to interact with Spring transactions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
public class TransactionUtil {

    /**
     * Run code after the outgoing transaction commits <br>
     * <b>NOTICE:</b> won't run on rollback
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @deprecated Use {@link com.kevinguanchedarias.owgejava.business.util.TransactionUtilService#doAfterCommit(Runnable)} as it's less expensive for unit testing
     */
    @Deprecated(since = "11.0.0")
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
