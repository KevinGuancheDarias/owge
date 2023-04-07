package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.mock.ConfigurationMock;
import com.kevinguanchedarias.owgejava.repository.ConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = ConfigurationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(ConfigurationRepository.class)
class ConfigurationBoTest {
    private final ConfigurationBo configurationBo;
    private final ConfigurationRepository configurationRepository;

    @Autowired
    ConfigurationBoTest(ConfigurationBo configurationBo, ConfigurationRepository configurationRepository) {
        this.configurationBo = configurationBo;
        this.configurationRepository = configurationRepository;
    }

    @Test
    void findOne_should_work() {
        var config = "FOO";
        var expectedRetVal = ConfigurationMock.givenConfiguration(config);
        given(configurationRepository.findById(config)).willReturn(Optional.of(expectedRetVal));


        assertThat(configurationBo.findOne(config))
                .isPresent()
                .contains(expectedRetVal);
    }

    @ParameterizedTest
    @CsvSource({
            "BAR,false",
            "FALSE,false",
            "FaLSe,false",
            "True,true",
            "TRUe,true"
    })
    void findBoolOrSetDefault_should_work(String value, boolean expected) {
        var name = "FOO";
        given(configurationRepository.findById(name)).willReturn(Optional.of(new Configuration(name, value)));

        assertThat(configurationBo.findBoolOrSetDefault(name, true)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "false,false",
            "true,true"
    })
    void findBoolOrSetDefault_handle_default_value(boolean defaultValue, boolean expected) {
        var name = "FOO";

        assertThat(configurationBo.findBoolOrSetDefault(name, defaultValue)).isEqualTo(expected);
    }
}
