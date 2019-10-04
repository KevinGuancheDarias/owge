package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectEntityRepository;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;

@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class RequirementInformationDaoTest {

	@Autowired
	private RequirementInformationDao requirementInformationDao;

	@Autowired
	private ObjectEntityRepository objectEntityRepository;

	@Autowired
	private ObjectRelationsRepository objectRelationsRepository;

	@Autowired
	private RequirementRepository requirementRepository;

	@Autowired
	private RequirementInformationRepository requirementInformationRepository;

	@Test
	public void shouldReturnRequirementsByType() {
		ObjectRelation objectRelation = new ObjectRelation();
		objectRelation.setObject(objectEntityRepository.findOne(RequirementTargetObject.UPGRADE.name()));
		objectRelation.setReferenceId(1);
		objectRelation = objectRelationsRepository.save(objectRelation);

		storeRandomRequiremenValuestOfGivenType(objectRelation, RequirementTypeEnum.BEEN_RACE, 4);
		assertEquals(4,
				requirementInformationDao.findRequirementsByType(objectRelation, RequirementTypeEnum.BEEN_RACE).size());
		storeRandomRequiremenValuestOfGivenType(objectRelation, RequirementTypeEnum.HAVE_SPECIAL_AVAILABLE, 2);
		assertEquals(2, requirementInformationDao
				.findRequirementsByType(objectRelation, RequirementTypeEnum.HAVE_SPECIAL_AVAILABLE).size());
		storeRandomRequiremenValuestOfGivenType(objectRelation, RequirementTypeEnum.HAVE_SPECIAL_ENABLED, 6);
		assertEquals(6, requirementInformationDao
				.findRequirementsByType(objectRelation, RequirementTypeEnum.HAVE_SPECIAL_ENABLED).size());

	}

	/**
	 * Stores random number for "second value" and "thir value"
	 * 
	 * @param relation
	 *            Persistent ObjectRelation
	 * @param type
	 * @param times
	 * @author Kevin Guanche Darias
	 */
	private void storeRandomRequiremenValuestOfGivenType(ObjectRelation relation, RequirementTypeEnum type, int times) {
		for (int i = 0; i < times; i++) {
			RequirementInformation requirementInformation = new RequirementInformation();
			requirementInformation.setRelation(relation);
			requirementInformation.setRequirement(requirementRepository.findOne(type.getValue()));
			requirementInformation.setSecondValue(RandomUtils.nextLong(1L, Long.MAX_VALUE));
			requirementInformation.setThirdValue(RandomUtils.nextLong());
			requirementInformationRepository.save(requirementInformation);
		}
	}

}
