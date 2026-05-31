package com.kevinguanchedarias.owgejava.business.mysql;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class MysqlLockState {
    /**
     * Intentionally a plain {@link ThreadLocal} and NOT an {@link InheritableThreadLocal}: MySQL
     * {@code GET_LOCK} locks are bound to the DB connection/session that took them, and a connection
     * is bound to a single thread for the duration of its transaction. When work is handed to another
     * thread (see {@code AsyncRunnerBo#runAsyncWithoutContext} and the delayed virtual-thread variant),
     * that thread runs on a different connection and must acquire its own locks. Inheriting the
     * parent's set would make the child believe it already holds locks that actually live on the
     * parent's session (so it would skip acquiring them), and would share a single {@link HashSet}
     * across threads, corrupting it under concurrent mutation.
     */
    private static final ThreadLocal<Set<String>> LOCKED_IDS_FOR_CURRENT_THREAD =
            ThreadLocal.withInitial(HashSet::new);

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
        return LOCKED_IDS_FOR_CURRENT_THREAD.get();
    }
}
