package com.kevinguanchedarias.owgejava.enumerations;

import org.junit.jupiter.api.Test;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectEntity;
import static org.assertj.core.api.Assertions.assertThat;

class ObjectEnumTest {

    @Test
    void isObject_should_work() {
        var instance = ObjectEnum.TIME_SPECIAL;
        var upgradeObject = givenObjectEntity();
        assertThat(instance.isObject(null)).isFalse();
        assertThat(instance.isObject(upgradeObject)).isFalse();
        assertThat(instance.isObject(givenObjectEntity(ObjectEnum.TIME_SPECIAL))).isTrue();
    }
}
