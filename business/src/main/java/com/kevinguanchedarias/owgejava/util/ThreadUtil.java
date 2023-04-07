package com.kevinguanchedarias.owgejava.util;

import lombok.experimental.UtilityClass;

/**
 * Overrides Thread static methods, to allow unit testing
 */
@UtilityClass
public class ThreadUtil {
    public static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    public static Thread currentThread() {
        return Thread.currentThread();
    }
}
