package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionConfigurationBo;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.ConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.kevinguanchedarias.owgejava.business.mission.MissionConfigurationBo.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = MissionConfigurationBo.class
)
@MockBean({
        ConfigurationRepository.class,
        ConfigurationBo.class
})
class MissionConfigurationBoTest {
    private final MissionConfigurationBo missionConfigurationBo;
    private final ConfigurationRepository configurationRepository;
    private final ConfigurationBo configurationBo;

    @Autowired
    public MissionConfigurationBoTest(
            ConfigurationBo configurationBo,
            ConfigurationRepository configurationRepository,
            MissionConfigurationBo missionConfigurationBo
    ) {
        this.configurationBo = configurationBo;
        this.configurationRepository = configurationRepository;
        this.missionConfigurationBo = missionConfigurationBo;
    }

    @Test
    void init_should_store_non_existing_configurations() {
        var existingConfigurations = List.of(
                Configuration.builder().name(MISSION_TIME_EXPLORE_KEY).value("12").build(),
                Configuration.builder().name(MISSION_TIME_GATHER_KEY).value("40").build()
        );
        given(configurationRepository.findByNameIn(anyList())).willReturn(existingConfigurations);
    }

    @Test
    void findMissionBaseTimeByType_should_properly_return_configured_explore_time() {
        var expectedValue = "42";
        var expectedConfiguration = Configuration.builder().name("foo").value(expectedValue).build();
        given(configurationBo.findOrSetDefault(MISSION_TIME_EXPLORE_KEY, DEFAULT_TIME_EXPLORE))
                .willReturn(expectedConfiguration);

        var retVal = missionConfigurationBo.findMissionBaseTimeByType(MissionType.EXPLORE);

        verify(configurationBo, times(1)).findOrSetDefault(MISSION_TIME_EXPLORE_KEY, DEFAULT_TIME_EXPLORE);
        assertEquals(expectedValue, retVal.toString());
    }
}
