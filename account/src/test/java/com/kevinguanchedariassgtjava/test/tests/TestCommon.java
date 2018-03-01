package com.kevinguanchedariassgtjava.test.tests;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.jpa.JpaSystemException;

import com.kevinguanchedarias.sgtjava.business.ConfigurationBo;
import com.kevinguanchedarias.sgtjava.business.UserBo;
import com.kevinguanchedarias.sgtjava.entity.Configuration;
import com.kevinguanchedarias.sgtjava.entity.Universe;
import com.kevinguanchedarias.sgtjava.entity.User;
import com.kevinguanchedarias.sgtjava.repository.ConfigurationRepository;
import com.kevinguanchedarias.sgtjava.repository.UniverseRepository;

public abstract class TestCommon {

	@Autowired
	private UniverseRepository universeRepository;

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private ConfigurationRepository configurationRepository;

	/**
	 * Will assert that Hibernate reports null values
	 * 
	 * @param sourceRepository
	 * @param entity
	 * @param field
	 *            - If null will check any field
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void checkNullSaveException(JpaRepository sourceRepository, Serializable entity, String field) {
		try {
			sourceRepository.save(entity);
			throw new RuntimeException("Should throw PropertyValueException exception");
		} catch (JpaSystemException e) {
			if (field != null) {
				assertTrue(e.getMessage().endsWith("." + field));
			} else {
				assertTrue(e.getMessage().indexOf("not-null property references a null or") != -1);
			}
		}

	}

	/**
	 * Prepares a valid universe (without null values)
	 * 
	 * @return
	 * @author Kevin Guanche Darias
	 */
	protected Universe prepareValidUniverse() {
		Universe universe = new Universe();
		universe.setName("Test universe");
		universe.setTargetDatabase("Complete lie");
		universe.setRestBaseUrl("http://test.com");
		return universe;
	}

	/**
	 * Will prepare a valid user
	 * 
	 * @return
	 * @author Kevin Guanche Darias
	 */
	protected User prepareValidUser() {
		User user = new User();
		user.setUsername("testUser");
		user.setEmail("testUser@kevinguanchedarias.com");

		user.setPassword("1234");
		return user;
	}

	/**
	 * Will persist multiple universes based on mode, changing only the
	 * <b>unique</b> keys
	 * 
	 * @param model
	 * @param times
	 *            - Number of items to add
	 * @author Kevin Guanche Darias
	 */
	protected void persistMultipleUniverses(Universe model, int times) {
		for (int i = 0; i < times; i++) {
			model.setName(model.getName() + " " + i);
			model.setRestBaseUrl("http//vendetutele.com/" + model.getName());
			universeRepository.save(model);
			model.setId(null);
		}
	}

	protected void putConfigJwtDuration() {
		Configuration durationConfig = new Configuration(UserBo.JWT_DURATION_CODE, "3600");
		configurationBo.save(durationConfig);
	}

	protected void putConfigJwtHashing() {
		Configuration durationConfig = new Configuration(UserBo.JWT_HASHING_ALGO, "HS256");
		configurationBo.save(durationConfig);
	}

	protected void putConfigJwtSecret() {
		Configuration durationConfig = new Configuration(UserBo.JWT_SECRET_DB_CODE, "TopSecret!");
		configurationBo.save(durationConfig);
	}

	/**
	 * Sets all JWT configuration
	 * 
	 * @author Kevin Guanche Darias
	 */
	protected void putConfigJwtAll() {
		putConfigJwtDuration();
		putConfigJwtHashing();
		putConfigJwtSecret();
	}

	protected void clearConfigCache() {
		configurationBo.clearCache();
		configurationRepository.deleteAll();
	}

	protected String genMissingConfigParamString(String name) {
		return "Configuration param " + name + " not found";
	}
}
