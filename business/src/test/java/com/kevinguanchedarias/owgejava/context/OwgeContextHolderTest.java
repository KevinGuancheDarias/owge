package com.kevinguanchedarias.owgejava.context;

import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.kevinguanchedarias.owgejava.mock.PlanetMock.SOURCE_PLANET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwgeContextHolderTest {

    private final OwgeContextHolder.OwgeContext expectedValue = new OwgeContextHolder.OwgeContext(SOURCE_PLANET_ID);

    @BeforeEach
    void clearContextHolder() {
        OwgeContextHolder.clear();
    }

    @Test
    void get_and_set() {
        assertThat(OwgeContextHolder.get()).isEmpty();
        OwgeContextHolder.set(expectedValue);

        assertThat(OwgeContextHolder.get()).contains(expectedValue);
    }

    @Test
    void set_should_throw_if_double_invoked() {
        OwgeContextHolder.set(expectedValue);

        assertThatThrownBy(() -> OwgeContextHolder.set(expectedValue))
                .isInstanceOf(ProgrammingException.class);
    }

    @Test
    void clear() {
        OwgeContextHolder.set(expectedValue);

        OwgeContextHolder.clear();

        assertThat(OwgeContextHolder.get()).isEmpty();

    }

    @AfterAll
    static void clearAfterAll() {
        OwgeContextHolder.clear();
    }
}
