package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.ConfigurationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 0.11.0
 */
@Service
@AllArgsConstructor
@Slf4j
public class MissionConfigurationBo {
    private final ConfigurationBo configurationBo;
    private final ConfigurationRepository configurationRepository;

    public static final String MISSION_TIME_EXPLORE_KEY = "MISSION_TIME_EXPLORE";
    public static final String MISSION_TIME_GATHER_KEY = "MISSION_TIME_GATHER";
    public static final String MISSION_TIME_ESTABLISH_BASE_KEY = "MISSION_TIME_ESTABLISH_BASE";
    public static final String MISSION_TIME_ATTACK_KEY = "MISSION_TIME_ATTACK";
    public static final String MISSION_TIME_CONQUEST_KEY = "MISSION_TIME_CONQUEST";
    public static final String MISSION_TIME_COUNTERATTACK_KEY = "MISSION_TIME_COUNTERATTACK";
    public static final String MISSION_TIME_DEPLOY_KEY = "MISSION_TIME_DEPLOY";

    public static final String DEFAULT_TIME_EXPLORE = "60";
    public static final String DEFAULT_TIME_GATHER = "900";
    public static final String DEFAULT_TIME_ESTABLISH_BASE = "43200";
    public static final String DEFAULT_TIME_ATTACK = "600";
    public static final String DEFAULT_TIME_CONQUEST = "86400";
    public static final String DEFAULT_TIME_COUNTERATTACK = "60";
    public static final String DEFAULT_TIME_DEPLOY = "60";

    private static class MissionTimeStore {
        private final String time;
        private final String description;

        public MissionTimeStore(String time, String description) {
            this.time = time;
            this.description = description;
        }

    }

    protected static final Map<String, MissionConfigurationBo.MissionTimeStore> MISSION_DEFAULT_CONFIG_STORE = new HashMap<>();

    static {
        MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_EXPLORE_KEY, new MissionConfigurationBo.MissionTimeStore(DEFAULT_TIME_EXPLORE, "Base explore time"));
        MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_GATHER_KEY, new MissionConfigurationBo.MissionTimeStore(DEFAULT_TIME_GATHER, "Base gather time"));
        MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_ESTABLISH_BASE_KEY,
                new MissionConfigurationBo.MissionTimeStore(DEFAULT_TIME_ESTABLISH_BASE, "Base establish base time"));
        MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_ATTACK_KEY, new MissionConfigurationBo.MissionTimeStore(DEFAULT_TIME_ATTACK, "Base attack time"));
        MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_CONQUEST_KEY,
                new MissionConfigurationBo.MissionTimeStore(DEFAULT_TIME_CONQUEST, "Base conquest time"));
        MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_COUNTERATTACK_KEY,
                new MissionConfigurationBo.MissionTimeStore(DEFAULT_TIME_COUNTERATTACK, "Base counterattack time"));
        MISSION_DEFAULT_CONFIG_STORE.put(MISSION_TIME_DEPLOY_KEY, new MissionConfigurationBo.MissionTimeStore(DEFAULT_TIME_DEPLOY, "Base deploy time"));
    }

    @PostConstruct
    void init() {
        insertMissionBaseTimeIfMissing();
    }

    public Long findMissionBaseTimeByType(MissionType type) {
        Long retVal;
        switch (type) {
            case EXPLORE:
                retVal = findMissionBaseTime(MISSION_TIME_EXPLORE_KEY);
                break;
            case GATHER:
                retVal = findMissionBaseTime(MISSION_TIME_GATHER_KEY);
                break;
            case ESTABLISH_BASE:
                retVal = findMissionBaseTime(MISSION_TIME_ESTABLISH_BASE_KEY);
                break;
            case ATTACK:
                retVal = findMissionBaseTime(MISSION_TIME_ATTACK_KEY);
                break;
            case COUNTERATTACK:
                retVal = findMissionBaseTime(MISSION_TIME_COUNTERATTACK_KEY);
                break;
            case CONQUEST:
                retVal = findMissionBaseTime(MISSION_TIME_CONQUEST_KEY);
                break;
            case DEPLOY:
                retVal = findMissionBaseTime(MISSION_TIME_DEPLOY_KEY);
                break;
            default:
                throw new SgtBackendInvalidInputException("Unsupported mission base time type, specified: " + type.name());
        }
        return retVal;
    }

    private List<Configuration> findAllMissionBaseTime() {
        return configurationRepository.findByNameIn(createMissionsKeyArray());
    }

    private Long findMissionBaseTime(String key, String defaultValue) {
        return Long.valueOf(configurationBo.findOrSetDefault(key, defaultValue).getValue());
    }

    /**
     * Finds the mission base time, if not set will return the default
     *
     * @param key The mission KEY, should use one of the constants available here
     * @throws ProgrammingException When key is not a vlid mission key
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
     * @since 0.8.1
     */
    private Long findMissionBaseTime(String key) {
        return findMissionBaseTime(key, MISSION_DEFAULT_CONFIG_STORE.get(key).time);
    }

    private void insertMissionBaseTimeIfMissing() {
        List<Configuration> storedValues = findAllMissionBaseTime();
        MISSION_DEFAULT_CONFIG_STORE.forEach((key, value) -> {
            if (storedValues.stream().noneMatch(current -> current.getName().equals(key))) {
                log.info("Mission time for " + key + " doesn't exists, adding default");
                doSaveDefaultMissionBaseTime(key);
            }
        });

    }

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

    private void doSaveDefaultMissionBaseTime(String key) {
        MissionTimeStore defaultValue = MISSION_DEFAULT_CONFIG_STORE.get(key);
        configurationBo.save(new Configuration(key, defaultValue.time, defaultValue.description));
    }
}
