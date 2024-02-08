package com.kevinguanchedarias.owgejava.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * Overrides Thread static methods, to allow unit testing
 */
@UtilityClass
public class ThreadUtil {

    @SneakyThrows
    public static void sleep(long millis) {
        Thread.sleep(millis);
    }

    public static Thread currentThread() {
        return Thread.currentThread();
    }
}
