package com.kevinguanchedarias.sgtjava.test.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.entity.MissionInformation;
import com.kevinguanchedarias.sgtjava.repository.MissionInformationRepository;

@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class MissionInformationTest extends TestCommon {

	@Autowired
	private MissionInformationRepository missionInformationRepository;

	@Test
	public void shouldNotSaveBecauseMissionIsNull() {
		MissionInformation missionInformation = prepareValidMissionInformation();
		missionInformation.setMission(null);
		checkNullSaveException(missionInformationRepository, missionInformation, "mission");
	}

	@Test
	public void shouldSave() {
		MissionInformation missionInformation = prepareValidMissionInformation();
		missionInformationRepository.save(missionInformation);
	}
}
