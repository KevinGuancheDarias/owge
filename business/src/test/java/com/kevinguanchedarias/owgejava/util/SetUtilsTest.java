package com.kevinguanchedarias.owgejava.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


class SetUtilsTest {

    @Test
    void getFirstElement_should_return_null_on_empty_set() {
        Set<String> set = Set.of();
        assertThat(SetUtils.getFirstElement(set)).isNull();
    }

    @Test
    void getFirstElement_should_return_null_on_null_entry() {
        Set<String> set = new HashSet<>();
        set.add(null);
        assertThat(SetUtils.getFirstElement(set)).isNull();
    }

    @Test
    void getFirstElement_should_work() {
        assertThat(SetUtils.getFirstElement(Set.of("foo", "bar"))).isNotNull();
    }
}
