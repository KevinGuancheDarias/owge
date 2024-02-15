package com.kevinguanchedarias.owgejava.business.mysql;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MysqlLockStateTest {
    private static final String TEST_KEY = "FooBarKey";
    private static final String TEST_KEY_2 = "SomethingTest";

    @AfterAll
    static void clearAtEnd() {
        MysqlLockState.clear();
    }

    @BeforeEach
    void clear() {
        MysqlLockState.clear();
    }

    @Test
    void addAll_and_get_should_work() {
        assertThat(MysqlLockState.get()).isNotNull().isEmpty();
        MysqlLockState.addAll(List.of(TEST_KEY));
        assertThat(MysqlLockState.get()).containsExactly(TEST_KEY);
        MysqlLockState.addAll(List.of(TEST_KEY_2));
        assertThat(MysqlLockState.get()).containsExactlyInAnyOrder(TEST_KEY, TEST_KEY_2);
    }

    @Test
    void clear_should_work() {
        MysqlLockState.addAll(List.of(TEST_KEY));
        MysqlLockState.clear();
        assertThat(MysqlLockState.get()).isEmpty();
    }

    @Test
    void removeAl_should_work() {
        MysqlLockState.addAll(List.of(TEST_KEY, TEST_KEY_2));
        MysqlLockState.removeAll(List.of(TEST_KEY));
        assertThat(MysqlLockState.get()).containsExactly(TEST_KEY_2);
    }
}
