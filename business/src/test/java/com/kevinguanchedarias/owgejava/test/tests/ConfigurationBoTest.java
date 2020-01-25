package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.repository.ConfigurationRepository;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationBoTest {
	private static final Configuration EXISTING_PROPERTY = new Configuration("EXISTING_PROPERTY", "74");

	@Mock
	private ConfigurationRepository configurationRepository;

	@InjectMocks
	private ConfigurationBo configurationBo;

	private HashMap<String, Configuration> serviceCache;
	private HashMap<String, Configuration> serviceCacheSpy;

	@Before
	public void beforeEach() {
		serviceCache = new HashMap<>();
		serviceCacheSpy = Mockito.spy(serviceCache);
		Mockito.when(configurationRepository.findOne(EXISTING_PROPERTY.getName())).thenReturn(EXISTING_PROPERTY);
		Whitebox.setInternalState(configurationBo, "cache", serviceCacheSpy);
	}

	@Test
	public void shouldSearchAndReturnValue() {
		configurationBo.findConfigurationParam(EXISTING_PROPERTY.getName());
		Mockito.verify(configurationRepository, Mockito.times(1)).findOne(EXISTING_PROPERTY.getName());
		Mockito.verify(serviceCacheSpy, Mockito.times(1)).put(EXISTING_PROPERTY.getName(), EXISTING_PROPERTY);
	}

	@Test
	public void shouldUseCacheWhenValueAlreadyExists() {
		configurationBo.findConfigurationParam(EXISTING_PROPERTY.getName());
		configurationBo.findConfigurationParam(EXISTING_PROPERTY.getName());
		Mockito.verify(serviceCacheSpy, Mockito.times(3)).get(EXISTING_PROPERTY.getName());
	}

	@Test
	public void shouldSave() {
		configurationBo.save(EXISTING_PROPERTY);
		Mockito.verify(configurationRepository, Mockito.times(1)).saveAndFlush(EXISTING_PROPERTY);
	}

	@Test
	public void shouldUseConfiguredValueForMissionWhenConfigured() {
		Mockito.when(configurationRepository.findOne(ConfigurationBo.MISSION_TIME_EXPLORE_KEY))
				.thenReturn(EXISTING_PROPERTY);
		Mockito.when(configurationRepository.findOne(ConfigurationBo.MISSION_TIME_GATHER_KEY))
				.thenReturn(EXISTING_PROPERTY);
		Mockito.when(configurationRepository.findOne(ConfigurationBo.MISSION_TIME_ESTABLISH_BASE_KEY))
				.thenReturn(EXISTING_PROPERTY);
		Mockito.when(configurationRepository.findOne(ConfigurationBo.MISSION_TIME_ATTACK_KEY))
				.thenReturn(EXISTING_PROPERTY);
		Mockito.when(configurationRepository.findOne(ConfigurationBo.MISSION_TIME_CONQUEST_KEY))
				.thenReturn(EXISTING_PROPERTY);
		Mockito.when(configurationRepository.findOne(ConfigurationBo.MISSION_TIME_COUNTERATTACK_KEY))
				.thenReturn(EXISTING_PROPERTY);

		assertEquals(Long.valueOf(EXISTING_PROPERTY.getValue()), configurationBo.findMissionExploreBaseTime());
		assertEquals(Long.valueOf(EXISTING_PROPERTY.getValue()), configurationBo.findMissionGatherBaseTime());
		assertEquals(Long.valueOf(EXISTING_PROPERTY.getValue()), configurationBo.findMissionEstablishBaseBaseTime());
		assertEquals(Long.valueOf(EXISTING_PROPERTY.getValue()), configurationBo.findMissionAttackBaseTime());
		assertEquals(Long.valueOf(EXISTING_PROPERTY.getValue()), configurationBo.findMissionConquestBaseTime());
		assertEquals(Long.valueOf(EXISTING_PROPERTY.getValue()), configurationBo.findMissionCounterattackBaseTime());
	}
}
