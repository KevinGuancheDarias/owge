package com.kevinguanchedarias.sgtjava.test.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.entity.MissionType;
import com.kevinguanchedarias.sgtjava.repository.MissionTypeRepository;

@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class MissionTypeTest extends TestCommon {

	@Autowired
	private MissionTypeRepository missionTypeRepository;

	@Test
	public void shouldNotSaveBecauseCodeIsNull() {
		MissionType missionType = prepareValidMissionType();
		missionType.setCode(null);
		checkNullSaveException(missionTypeRepository, missionType, "code");
	}

	@Test
	public void shouldNotSaveBecauseDescriptionIsNull() {
		MissionType missionType = prepareValidMissionType();
		missionType.setDescription(null);
		checkNullSaveException(missionTypeRepository, missionType, "description");
	}

	@Test
	public void shouldNotSaveBecauseIsSharedIsNull() {
		MissionType missionType = prepareValidMissionType();
		missionType.setIsShared(null);
		checkNullSaveException(missionTypeRepository, missionType, "isShared");
	}

	@Test
	public void shouldSave() {
		MissionType missionType = prepareValidMissionType();
		missionTypeRepository.save(missionType);
	}
}
