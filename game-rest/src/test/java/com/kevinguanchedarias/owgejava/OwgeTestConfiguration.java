package com.kevinguanchedarias.owgejava;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import com.kevinguanchedarias.owgejava.security.AdminTokenConfigLoader;
import com.kevinguanchedarias.owgejava.util.GitUtilService;
import lombok.AllArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ComponentScan(basePackageClasses = GitUtilService.class)
@MockBean({
        ConfigurationBo.class,
        AdminTokenConfigLoader.class,
        UserStorageBo.class,
        GalaxyBo.class,
        ObtainedUnitTemporalInformationRepository.class
})
@AllArgsConstructor
public class OwgeTestConfiguration {

    private final ConfigurationBo configurationBo;

    @PostConstruct
    void configureMock() {
        when(configurationBo.findOrSetDefault(any(), any())).thenAnswer(this::answerConfiguration);
    }

    private Configuration answerConfiguration(InvocationOnMock invocation) {
        com.kevinguanchedarias.owgejava.entity.Configuration configuration = new com.kevinguanchedarias.owgejava.entity.Configuration();
        configuration.setName(invocation.getArgument(0));
        configuration.setValue(invocation.getArgument(1));
        return configuration;
    }
}
