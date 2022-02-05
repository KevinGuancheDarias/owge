package com.kevinguanchedarias.owgejava.context;

import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AttackInformationContextHolder {
    private static final ThreadLocal<AttackInformation> CONTEXT = new InheritableThreadLocal<>();

    public static void setContext(AttackInformation attackInformation) {
        CONTEXT.remove();
        CONTEXT.set(attackInformation);
    }

    public static AttackInformation getContext() {
        return CONTEXT.get();
    }
}
