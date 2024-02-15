package com.kevinguanchedarias.owgejava.business.util;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 */
@Service
public class TransactionUtilService {
    private static final String ALREADY_COMMITTING_KEY = "owge_already_committing";

    /**
     * Runs lambda using a transaction with propagation REQUIRED
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     */
    @Transactional
    public void runWithRequired(Runnable action) {
        action.run();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runWithRequiresNew(Runnable action) {
        action.run();
    }

    /**
     * <b>Important: If the logic inside the after commit has modifications to the DB, they wouldn't work even if @Transactional is used,
     * but won't fail, if logic to modify data is going to happen, ensure it has a brand new transaction (with Requires new)</b>
     */
    public void doAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.hasResource(ALREADY_COMMITTING_KEY)) {
            action.run();
        } else {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (!TransactionSynchronizationManager.hasResource(ALREADY_COMMITTING_KEY)) {
                        TransactionSynchronizationManager.bindResource(ALREADY_COMMITTING_KEY, true);
                    }
                    action.run();
                }
            });
        }
    }
    
    public void doAfterCompletion(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                action.run();
            }
        });
    }
}
