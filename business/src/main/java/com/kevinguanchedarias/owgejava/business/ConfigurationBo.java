package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
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

	public static final String MISSION_TIME_EXPLORE_KEY = "MISSION_TIME_EXPLORE";
	public static final String MISSION_TIME_GATHER_KEY = "MISSION_TIME_GATHER";
	public static final String MISSION_TIME_ESTABLISH_BASE_KEY = "MISSION_TIME_ESTABLISH_BASE";
	public static final String MISSION_TIME_ATTACK_KEY = "MISSION_TIME_ATTACK";
	public static final String MISSION_TIME_CONQUEST_KEY = "MISSION_TIME_CONQUEST";
	public static final String MISSION_TIME_COUNTERATTACK_KEY = "MISSION_TIME_COUNTERATTACK";
	public static final String MISSION_TIME_DEPLOY_KEY = "MISSION_TIME_DEPLOY";

	protected static final Map<String, MissionTimeStore> MISSION_DEFAULT_CONFIG_STORE = new HashMap<>();

	static {
		MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_EXPLORE_KEY, new MissionTimeStore("60", "Base explore time"));
		MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_GATHER_KEY, new MissionTimeStore("900", "Base gather time"));
		MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_ESTABLISH_BASE_KEY,
				new MissionTimeStore("43200", "Base establish base time"));
		MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_ATTACK_KEY, new MissionTimeStore("600", "Base attack time"));
		MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_CONQUEST_KEY,
				new MissionTimeStore("86400", "Base conquest time"));
		MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_COUNTERATTACK_KEY,
				new MissionTimeStore("60", "Base counterattack time"));
		MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_DEPLOY_KEY, new MissionTimeStore("60", "Base deploy time"));
	}

	public static final Integer MISSION_NUMBER_OF_MISSIONS = 6;

	private static class MissionTimeStore {
		private String time;
		private String description;

		public MissionTimeStore(String time, String description) {
			this.time = time;
			this.description = description;
		}

	}

	@Autowired
	private ConfigurationRepository configurationRepository;

	@PostConstruct
	public void init() {
		insertMissionBaseTimeIfMissing();
		if (!findOne("UNIVERSE_ID").isPresent()) {
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
	 * @param name
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
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
	 * @return
	 * @since 0.7.5
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<Configuration> findPrivilegedReadOnly() {
		List<String> readOnlyPrivileged = new ArrayList<>();
		readOnlyPrivileged.add(WEBSOCKET_ENDPOINT_KEY);
		return configurationRepository.findByNameIn(readOnlyPrivileged);
	}

	/**
	 * Will find the configuration param from cache, else from database
	 *
	 * @param name
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
	 * @param name
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
	 */
	public void deleteOne(String name) {
		configurationRepository.deleteById(name);
	}

	public Configuration saveByKeyAndValue(String key, String value) {
		return save(new Configuration(key, value));
	}

	public List<Configuration> findAllMissionBaseTime() {
		return configurationRepository.findByNameIn(createMissionsKeyArray());
	}

	/**
	 * Finds the base time by enumeration
	 *
	 * @deprecated Use findmissionBaseTime(String)
	 * @param type mission base time enum
	 * @return the computed base time value
	 * @throws SgtBackendInvalidInputException MissionType not supported
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.1")
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

	/**
	 * Finds the mission base time, if not set will return the default
	 *
	 * @since 0.8.1
	 * @param key The mission KEY, should use one of the constants available here
	 * @return
	 * @throws ProgrammingException When key is not a vlid mission key
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
	 */
	public Long findMissionBaseTime(String key) {
		if (MISSION_DEFAULT_CONFIG_STORE.containsKey(key)) {
			return findMissionBaseTime(key, MISSION_DEFAULT_CONFIG_STORE.get(key).time);
		} else {
			throw new ProgrammingException("key " + key + " is not a valid mission key");
		}
	}

	/**
	 *
	 * @deprecated Use findMissionBaseTime(String)
	 * @return
	 */
	@Deprecated(since = "0.8.1")
	public Long findMissionExploreBaseTime() {
		return findMissionBaseTime(MISSION_TIME_EXPLORE_KEY,
				MISSION_DEFAULT_CONFIG_STORE.get(MISSION_TIME_EXPLORE_KEY).time);
	}

	/**
	 *
	 * @deprecated Use findMissionBaseTime(String)
	 * @return
	 */
	@Deprecated(since = "0.8.1")
	public Long findMissionGatherBaseTime() {
		return findMissionBaseTime(MISSION_TIME_GATHER_KEY,
				MISSION_DEFAULT_CONFIG_STORE.get(MISSION_TIME_GATHER_KEY).time);
	}

	/**
	 *
	 * @deprecated Use findMissionBaseTime(String)
	 * @return
	 */
	@Deprecated(since = "0.8.1")
	public Long findMissionEstablishBaseBaseTime() {
		return findMissionBaseTime(MISSION_TIME_ESTABLISH_BASE_KEY,
				MISSION_DEFAULT_CONFIG_STORE.get(MISSION_TIME_ESTABLISH_BASE_KEY).time);
	}

	/**
	 *
	 * @deprecated Use findMissionBaseTime(String)
	 * @return
	 */
	@Deprecated(since = "0.8.1")
	public Long findMissionAttackBaseTime() {
		return findMissionBaseTime(MISSION_TIME_ATTACK_KEY,
				MISSION_DEFAULT_CONFIG_STORE.get(MISSION_TIME_ATTACK_KEY).time);
	}

	/**
	 *
	 * @deprecated Use findMissionBaseTime(String)
	 * @return
	 */
	@Deprecated(since = "0.8.1")
	public Long findMissionConquestBaseTime() {
		return findMissionBaseTime(MISSION_TIME_CONQUEST_KEY,
				MISSION_DEFAULT_CONFIG_STORE.get(MISSION_TIME_CONQUEST_KEY).time);
	}

	/**
	 *
	 * @deprecated Use findMissionBaseTime(String)
	 * @return
	 */
	@Deprecated(since = "0.8.1")
	public Long findMissionCounterattackBaseTime() {
		return findMissionBaseTime(MISSION_TIME_COUNTERATTACK_KEY,
				MISSION_DEFAULT_CONFIG_STORE.get(MISSION_TIME_COUNTERATTACK_KEY).time);
	}

	/**
	 *
	 * @deprecated Use findMissionBaseTime(String)
	 * @return
	 */
	@Deprecated(since = "0.8.1")
	public Long findMissionDeployBaseTime() {
		return findMissionBaseTime(MISSION_TIME_DEPLOY_KEY,
				MISSION_DEFAULT_CONFIG_STORE.get(MISSION_TIME_DEPLOY_KEY).time);
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
		} catch (SgtBackendConfigurationNotFoundException | NoSuchElementException e) {
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

	private void insertMissionBaseTimeIfMissing() {
		List<Configuration> storedValues = findAllMissionBaseTime();
		MISSION_DEFAULT_CONFIG_STORE.forEach((key, value) -> {
			if (storedValues.stream().noneMatch(current -> current.getName().equals(key))) {
				LOCAL_LOGGER.info("Mission time for " + key + " doesn't exists, adding default");
				doSaveDefaultMissionBaseTime(key);
			}
		});

	}

	/**
	 * @return
	 */
	private ArrayList<String> createMissionsKeyArray() {
		ArrayList<String> missionKeys = new ArrayList<>();
		missionKeys.add(MISSION_TIME_EXPLORE_KEY);
		missionKeys.add(MISSION_TIME_GATHER_KEY);
		missionKeys.add(MISSION_TIME_ESTABLISH_BASE_KEY);
		missionKeys.add(MISSION_TIME_ATTACK_KEY);
		missionKeys.add(MISSION_TIME_CONQUEST_KEY);
		missionKeys.add(MISSION_TIME_COUNTERATTACK_KEY);
		missionKeys.add(MISSION_TIME_DEPLOY_KEY);
		return missionKeys;
	}

	private Long findMissionBaseTime(String key, String defaultValue) {
		return Long.valueOf(findOrSetDefault(key, defaultValue).getValue());
	}

	private Configuration doSaveDefaultMissionBaseTime(String key) {
		MissionTimeStore defaultValue = MISSION_DEFAULT_CONFIG_STORE.get(key);
		return save(new Configuration(key, defaultValue.time, defaultValue.description));
	}

	/**
	 * Checks if the configuration refers to the base time of a mission
	 *
	 * @param configuration
	 * @throws SgtBackendInvalidInputException Number is below the expected value
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
