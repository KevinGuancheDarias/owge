package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.repository.MissionInformationRepository;
import com.kevinguanchedarias.sgtjava.repository.MissionRepository;

@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class MissionTest extends TestCommon {

	@Autowired
	private MissionRepository missionRepository;

	@Autowired
	private MissionInformationRepository missionInformationRepository;

	@Test
	public void shouldNotSaveBecauseTypeIsNull() {
		Mission mission = prepareValidMission();
		mission.setType(null);
		checkNullSaveException(missionRepository, mission, "type");
	}

	@Test
	public void shouldNotSaveBecauseTerminationDateIsNull() {
		Mission mission = prepareValidMission();
		mission.setTerminationDate(null);
		checkNullSaveException(missionRepository, mission, "terminationDate");
	}

	@Test
	public void shouldSaveMissionInformationWhenSavingMission() {
		Mission mission = prepareValidMission();
		mission.setMissionInformation(prepareValidMissionInformation());
		missionRepository.save(mission);
		assertEquals(1, missionInformationRepository.findAll().size());
	}

	@Test
	public void shouldSave() {
		persistValidMission();
	}
}
