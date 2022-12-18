package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.mock.ConfigurationMock;
import com.kevinguanchedarias.owgejava.repository.ConfigurationRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

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
        

        Assertions.assertThat(configurationBo.findOne(config))
                .isPresent()
                .contains(expectedRetVal);
    }
}
