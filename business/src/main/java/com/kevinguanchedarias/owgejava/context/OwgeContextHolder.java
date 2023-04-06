package com.kevinguanchedarias.owgejava.context;

import com.kevinguanchedarias.owgejava.exception.ProgrammingException;

import java.util.Optional;

public class OwgeContextHolder {
    private static final ThreadLocal<Optional<OwgeContext>> OWGE_CONTEXT = ThreadLocal.withInitial(Optional::empty);

    public record OwgeContext(Long selectedPlanetId) {
    }

    public static Optional<OwgeContext> get() {
        return OWGE_CONTEXT.get();
    }

    public static void set(OwgeContext owgeContext) {
        if (get().isPresent()) {
            throw new ProgrammingException("You should never manually invoke the get");
        }
        OWGE_CONTEXT.set(Optional.ofNullable(owgeContext));
    }

    public static void clear() {
        OWGE_CONTEXT.remove();
    }
}
