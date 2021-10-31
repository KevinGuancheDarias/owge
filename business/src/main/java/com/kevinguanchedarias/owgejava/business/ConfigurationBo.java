package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendConfigurationNotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.ConfigurationRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConfigurationBo implements Serializable {
    private static final long serialVersionUID = -9297616911916053L;

    private static final Logger LOCAL_LOGGER = Logger.getLogger(ConfigurationBo.class);

    public static final String WEBSOCKET_ENDPOINT_KEY = "WEBSOCKET_ENDPOINT";

    public static final String MISSION_TIME_INDEX_OF_KEY = "MISSION_TIME_";
    public static final Long MISSION_TIME_MINIMUM_VALUE = 10L;


    @Autowired
    private ConfigurationRepository configurationRepository;

    @PostConstruct
    public void init() {
        if (findOne("UNIVERSE_ID").isEmpty()) {
            LOCAL_LOGGER.info("Adding UNIVERSE_ID as it's missing");
            Configuration configuration = new Configuration();
            configuration.setName("UNIVERSE_ID");
            configuration.setValue(UUID.randomUUID().toString());
            configuration.setPrivileged(true);
            save(configuration);
        }
    }

    /**
     * Finds one entity, while
     * {@link ConfigurationBo#findConfigurationParam(String)} works, this one may
     * return null, instead of throwing
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
     * @since 0.9.0
     */
    public Optional<Configuration> findOne(String name) {
        return configurationRepository.findById(name);
    }

    public List<Configuration> findAllNonPrivileged() {
        return configurationRepository.findByPrivilegedFalse();
    }

    /**
     * Returns privileged properties that may be read from outside systems, ex:
     * WEBSOCKET_ENDPOINT
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.5
     */
    public List<Configuration> findPrivilegedReadOnly() {
        List<String> readOnlyPrivileged = new ArrayList<>();
        readOnlyPrivileged.add(WEBSOCKET_ENDPOINT_KEY);
        return configurationRepository.findByNameIn(readOnlyPrivileged);
    }

    /**
     * Will find the configuration param from cache, else from database
     *
     * @return Configuration param
     * @throws SgtBackendConfigurationNotFoundException when the param doesn't
     *                                                  exists
     * @author Kevin Guanche Darias
     */
    public Configuration findConfigurationParam(String name) {
        Configuration retVal = configurationRepository.findById(name).orElse(null);

        if (retVal == null) {
            throw new SgtBackendConfigurationNotFoundException("Configuration param " + name + " not found");
        }

        return retVal;
    }

    public Configuration save(Configuration configuration) {
        if (isOfTypeMissionTime(configuration)) {
            checkCanSaveMisisonTyme(configuration);
        }
        return configurationRepository.saveAndFlush(configuration);
    }

    /**
     * Deletes one instance
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
     * @since 0.9.0
     */
    public void deleteOne(String name) {
        configurationRepository.deleteById(name);
    }

    public void saveByKeyAndValue(String key, String value) {
        save(new Configuration(key, value));
    }

    public Configuration findOrSetDefault(String name, String defaultValue) {
        try {
            return findConfigurationParam(name);
        } catch (SgtBackendConfigurationNotFoundException | NoSuchElementException e) {
            LOCAL_LOGGER.warn("Warning, configuration not found, using default " + defaultValue
                    + ", nested message is: " + e.getMessage());
            return new Configuration(name, defaultValue);
        }
    }

    public int findIntOrSetDefault(String name, String defaultValue) {
        return Integer.parseInt(findOrSetDefault(name, defaultValue).getValue());
    }

    /**
     * Finds the current value for DEPLOYMENT_CONFIG
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    public DeployMissionConfigurationEnum findDeployMissionConfiguration() {
        DeployMissionConfigurationEnum value;
        try {
            value = DeployMissionConfigurationEnum.valueOf(
                    findOrSetDefault("DEPLOYMENT_CONFIG", DeployMissionConfigurationEnum.FREEDOM.name()).getValue());
        } catch (Exception e) {
            LOCAL_LOGGER.warn(
                    "Invalid value for DEPLOYMENT_CONFIG, please check DeployMissionConfigurationEnum for valid values, Defaulting to FREEDOM");
            value = DeployMissionConfigurationEnum.FREEDOM;
        }
        return value;
    }


    /**
     * Checks if the configuration refers to the base time of a mission
     *
     * @throws SgtBackendInvalidInputException Number is below the expected value
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private boolean isOfTypeMissionTime(Configuration configuration) {
        return configuration.getName() != null && configuration.getName().indexOf(MISSION_TIME_INDEX_OF_KEY) == 0;
    }

    private void checkCanSaveMisisonTyme(Configuration configuration) {
        long value;
        try {
            value = Long.parseLong(configuration.getValue());
        } catch (Exception e) {
            value = 0L;
        }
        if (value < MISSION_TIME_MINIMUM_VALUE) {
            throw new SgtBackendInvalidInputException("Invalid value " + configuration.getValue() + " for param "
                    + configuration.getName() + " the value must be " + MISSION_TIME_MINIMUM_VALUE + " or grater");
        }
    }
}
