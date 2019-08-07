package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendConfigurationNotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.ConfigurationRepository;

@Service
public class ConfigurationBo implements Serializable {
	private static final long serialVersionUID = -9297616911916053L;

	private static final Logger LOCAL_LOGGER = Logger.getLogger(ConfigurationBo.class);

	public static final String ACCOUNT_ENDPOINT_KEY = "ACCOUNT_LOGIN_URL";
	public static final String WEBSOCKET_ENDPOINT_KEY = "WEBSOCKET_ENDPOINT";

	public static final String SYSTEM_EMAIL_KEY = "SYSTEM_EMAIL";
	public static final String SYSTEM_SECRET_KEY = "SYSTEM_PASSWORD";

	public static final String MISSION_TIME_INDEX_OF_KEY = "MISSION_TIME_";
	public static final Long MISSION_TIME_MINIMUM_VALUE = 10L;

	public static final String MISSION_DEFAULT_TIME_EXPLORE = "60";
	public static final String MISSION_TIME_EXPLORE_KEY = "MISSION_TIME_EXPLORE";
	public static final String MISSION_TIME_EXPLORE_DISPLAY_NAME = "Tiempo base explorar";

	public static final String MISSION_DEFAULT_TIME_GATHER = "900";
	public static final String MISSION_TIME_GATHER_KEY = "MISSION_TIME_GATHER";
	public static final String MISSION_TIME_GATHER_DISPLAY_NAME = "Tiempo base recolectar";

	public static final String MISSION_DEFAULT_TIME_ESTABLISH_BASE = "43200";
	public static final String MISSION_TIME_ESTABLISH_BASE_KEY = "MISSION_TIME_ESTABLISH_BASE";
	public static final String MISSION_TIME_ESTABLISH_BASE_DISPLAY_NAME = "Tiempo base establecer base";

	public static final String MISSION_DEFAULT_TIME_ATTACK = "600";
	public static final String MISSION_TIME_ATTACK_KEY = "MISSION_TIME_ATTACK";
	public static final String MISSION_TIME_ATTACK_DISPLAY_NAME = "Tiempo base atacar";

	public static final String MISSION_DEFAULT_TIME_CONQUEST = "900";
	public static final String MISSION_TIME_CONQUEST_KEY = "MISSION_TIME_CONQUEST";
	public static final String MISSION_TIME_CONQUEST_DISPLAY_NAME = "Tiempo base conquistar";

	public static final String MISSION_DEFAULT_TIME_COUNTERATTACK = "900";
	public static final String MISSION_TIME_COUNTERATTACK_KEY = "MISSION_TIME_COUNTERATTACK";
	public static final String MISSION_TIME_COUNTERATTACK_DISPLAY_NAME = "Tiempo base contratacar";

	public static final String MISSION_DEFAULT_TIME_DEPLOY = "60";
	public static final String MISSION_TIME_DEPLOY_KEY = "MISSION_TIME_DEPLOY";
	public static final String MISSION_TIME_DEPLOY_DISPLAY_NAME = "Base time deploy";

	public static final Integer MISSION_NUMBER_OF_MISSIONS = 6;

	private HashMap<String, Configuration> cache = new HashMap<>();

	@Autowired
	private ConfigurationRepository configurationRepository;

	@PostConstruct
	public void init() {
		insertMissionBaseTimeIfMissing();
	}

	public List<Configuration> findAllNonPrivileged() {
		return configurationRepository.findByPrivilegedFalse();
	}

	/**
	 * Will find the configuration param from cache, else from database
	 * 
	 * @param name
	 * @return Configuration param
	 * @author Kevin Guanche Darias
	 */
	public Configuration findConfigurationParam(String name) {
		if (cache.get(name) != null) {
			return cache.get(name);
		}

		Configuration retVal = configurationRepository.findOne(name);

		if (retVal == null) {
			throw new SgtBackendConfigurationNotFoundException("Configuration param " + name + " not found");
		}

		cache.put(retVal.getName(), retVal);
		return retVal;
	}

	public Configuration save(Configuration configuration) {
		if (isOfTypeMissionTime(configuration)) {
			checkCanSaveMisisonTyme(configuration);
		}
		return configurationRepository.saveAndFlush(configuration);
	}

	public void clearCache() {
		cache = new HashMap<>();
	}

	public Configuration saveByKeyAndValue(String key, String value) {
		return save(new Configuration(key, value));
	}

	public List<Configuration> findAllMissionBaseTime() {
		ArrayList<String> missionKeys = new ArrayList<>();
		missionKeys.add(MISSION_TIME_EXPLORE_KEY);
		missionKeys.add(MISSION_TIME_GATHER_KEY);
		missionKeys.add(MISSION_TIME_ESTABLISH_BASE_KEY);
		missionKeys.add(MISSION_TIME_ATTACK_KEY);
		missionKeys.add(MISSION_TIME_CONQUEST_KEY);
		missionKeys.add(MISSION_TIME_COUNTERATTACK_KEY);
		missionKeys.add(MISSION_TIME_DEPLOY_KEY);
		return configurationRepository.findByNameIn(missionKeys);
	}

	/**
	 * Finds the base time by enumeration
	 * 
	 * @param type
	 *            mission base time enum
	 * @return the computed base time value
	 * @throws SgtBackendInvalidInputException
	 *             MissionType not supported
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long findMissionBaseTimeByType(MissionType type) {
		Long retVal;
		switch (type) {
		case EXPLORE:
			retVal = findMissionExploreBaseTime();
			break;
		case GATHER:
			retVal = findMissionGatherBaseTime();
			break;
		case ESTABLISH_BASE:
			retVal = findMissionEstablishBaseBaseTime();
			break;
		case ATTACK:
			retVal = findMissionAttackBaseTime();
			break;
		case COUNTERATTACK:
			retVal = findMissionCounterattackBaseTime();
			break;
		case CONQUEST:
			retVal = findMissionConquestBaseTime();
			break;
		case DEPLOY:
			retVal = findMissionDeployBaseTime();
			break;
		default:
			throw new SgtBackendInvalidInputException("Unsupported mission base time type, specified: " + type.name());
		}
		return retVal;
	}

	public Long findMissionExploreBaseTime() {
		return findMissionBaseTime(MISSION_TIME_EXPLORE_KEY, MISSION_DEFAULT_TIME_EXPLORE);
	}

	public Long findMissionGatherBaseTime() {
		return findMissionBaseTime(MISSION_TIME_GATHER_KEY, MISSION_DEFAULT_TIME_GATHER);
	}

	public Long findMissionEstablishBaseBaseTime() {
		return findMissionBaseTime(MISSION_TIME_ESTABLISH_BASE_KEY, MISSION_DEFAULT_TIME_ESTABLISH_BASE);
	}

	public Long findMissionAttackBaseTime() {
		return findMissionBaseTime(MISSION_TIME_ATTACK_KEY, MISSION_DEFAULT_TIME_ATTACK);
	}

	public Long findMissionConquestBaseTime() {
		return findMissionBaseTime(MISSION_TIME_CONQUEST_KEY, MISSION_DEFAULT_TIME_CONQUEST);
	}

	public Long findMissionCounterattackBaseTime() {
		return findMissionBaseTime(MISSION_TIME_COUNTERATTACK_KEY, MISSION_DEFAULT_TIME_COUNTERATTACK);
	}

	public Long findMissionDeployBaseTime() {
		return findMissionBaseTime(MISSION_TIME_DEPLOY_KEY, MISSION_DEFAULT_TIME_DEPLOY);
	}

	/**
	 * Checks if the input email is system Email
	 * 
	 * @param email
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean isSystemEmail(String email) {
		return email.equals(findConfigurationParam(SYSTEM_EMAIL_KEY).getValue());
	}

	/**
	 * Checks if the password is the admin password
	 * 
	 * @param password
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean isSystemPassword(String password) {
		return password.equals(findConfigurationParam(SYSTEM_SECRET_KEY).getValue());
	}

	public Configuration findOrSetDefault(String name, String defaultValue) {
		try {
			return findConfigurationParam(name);
		} catch (SgtBackendConfigurationNotFoundException e) {
			LOCAL_LOGGER.warn("Warning, configuration not found, using default " + defaultValue
					+ ", nested message is: " + e.getMessage());
			return new Configuration(name, defaultValue);
		}
	}

	/**
	 * Finds the current value for DEPLOYMENT_CONFIG
	 * 
	 * @return
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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

	private Configuration saveMissionExploreBaseTime(Long value) {
		return doSaveMissionBaseTime(MISSION_TIME_EXPLORE_KEY, value, MISSION_TIME_EXPLORE_DISPLAY_NAME);
	}

	private Configuration saveMissionGatherBaseTime(Long value) {
		return doSaveMissionBaseTime(MISSION_TIME_GATHER_KEY, value, MISSION_TIME_GATHER_DISPLAY_NAME);
	}

	private Configuration saveMissionEstablishBaseBaseTime(Long value) {
		return doSaveMissionBaseTime(MISSION_TIME_ESTABLISH_BASE_KEY, value, MISSION_TIME_ESTABLISH_BASE_DISPLAY_NAME);
	}

	private Configuration saveMissionAttackBaseTime(Long value) {
		return doSaveMissionBaseTime(MISSION_TIME_ATTACK_KEY, value, MISSION_TIME_ATTACK_DISPLAY_NAME);
	}

	private Configuration saveMissionConquestBaseTime(Long value) {
		return doSaveMissionBaseTime(MISSION_TIME_CONQUEST_KEY, value, MISSION_TIME_CONQUEST_DISPLAY_NAME);
	}

	private Configuration saveMissionCounterattackBaseTime(Long value) {
		return doSaveMissionBaseTime(MISSION_TIME_COUNTERATTACK_KEY, value, MISSION_TIME_COUNTERATTACK_DISPLAY_NAME);
	}

	private Long findMissionBaseTime(String key, String defaultValue) {
		return Long.valueOf(findOrSetDefault(key, defaultValue).getValue());
	}

	private Configuration doSaveMissionBaseTime(String key, Long value, String displayName) {
		return save(new Configuration(key, String.valueOf(value), displayName));
	}

	private void insertMissionBaseTimeIfMissing() {
		if (!isAccountProject() && findAllMissionBaseTime().size() != MISSION_NUMBER_OF_MISSIONS) {
			LOCAL_LOGGER.info(
					"Notice: missions base times, not matching expected number of entries in config, adding with default");

			saveMissionExploreBaseTime(Long.valueOf(MISSION_DEFAULT_TIME_EXPLORE));
			saveMissionGatherBaseTime(Long.valueOf(MISSION_DEFAULT_TIME_GATHER));
			saveMissionEstablishBaseBaseTime(Long.valueOf(MISSION_DEFAULT_TIME_ESTABLISH_BASE));
			saveMissionAttackBaseTime(Long.valueOf(MISSION_DEFAULT_TIME_ATTACK));
			saveMissionConquestBaseTime(Long.valueOf(MISSION_DEFAULT_TIME_CONQUEST));
			saveMissionCounterattackBaseTime(Long.valueOf(MISSION_DEFAULT_TIME_COUNTERATTACK));
		}
	}

	private boolean isAccountProject() {
		return configurationRepository.findOne("JWT_ALGO") != null;
	}

	/**
	 * Checks if the configuration refers to the base time of a mission
	 * 
	 * @param configuration
	 * @throws SgtBackendInvalidInputException
	 *             Number is below the expected value
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private boolean isOfTypeMissionTime(Configuration configuration) {
		return configuration.getName() != null && configuration.getName().indexOf(MISSION_TIME_INDEX_OF_KEY) == 0;
	}

	private void checkCanSaveMisisonTyme(Configuration configuration) {
		Long value;
		try {
			value = Long.valueOf(configuration.getValue());
		} catch (Exception e) {
			value = 0L;
		}
		if (value < MISSION_TIME_MINIMUM_VALUE) {
			throw new SgtBackendInvalidInputException("Invalid value " + configuration.getValue() + " for param "
					+ configuration.getName() + " the value must be " + MISSION_TIME_MINIMUM_VALUE + " or grater");
		}
	}
}
