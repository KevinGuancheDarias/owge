package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Configuration;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConfigurationMock {
    public static Configuration givenConfigurationTrue() {
        return givenConfiguration("TRUE");
    }

    public static Configuration givenConfigurationFalse() {
        return givenConfiguration("FALSE");
    }

    public static Configuration givenConfiguration(String value) {
        return Configuration.builder()
                .value(value)
                .build();
    }
}
