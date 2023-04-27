package com.kevinguanchedarias.owgejava.context;

import org.springframework.transaction.TransactionStatus;

import java.util.Optional;

public class OwgeContextHolder {
    private static final ThreadLocal<Optional<OwgeContext>> OWGE_CONTEXT = ThreadLocal.withInitial(Optional::empty);

    public record OwgeContext(Long selectedPlanetId, TransactionStatus transactionStatus) {
    }

    public static Optional<OwgeContext> get() {
        return OWGE_CONTEXT.get();
    }

    public static void set(OwgeContext owgeContext) {
        OWGE_CONTEXT.set(Optional.ofNullable(owgeContext));
    }

    public static void clear() {
        OWGE_CONTEXT.remove();
    }
}
