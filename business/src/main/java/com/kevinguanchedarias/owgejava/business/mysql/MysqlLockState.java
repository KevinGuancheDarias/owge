package com.kevinguanchedarias.owgejava.business.mysql;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class MysqlLockState {
    private static final InheritableThreadLocal<Set<String>> LOCKED_IDS_FOR_CURRENT_THREAD = new InheritableThreadLocal<>();

    public static void addAll(List<String> ids) {
        get().addAll(ids);
    }

    public static void removeAll(List<String> id) {
        id.forEach(get()::remove);
    }

    public static void clear() {
        LOCKED_IDS_FOR_CURRENT_THREAD.remove();
    }

    public static Set<String> get() {
        Set<String> instance;
        if ((instance = LOCKED_IDS_FOR_CURRENT_THREAD.get()) == null) {
            instance = new HashSet<>();
            LOCKED_IDS_FOR_CURRENT_THREAD.set(instance);
        }
        return instance;
    }
}
