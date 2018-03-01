package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.entity.Requirement;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementType;
import com.kevinguanchedarias.sgtjava.repository.RequirementRepository;

@ActiveProfiles("dev")
@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class RequirementTypeTest {

	@Autowired
	private RequirementRepository requirementRepository;

	@Test
	public void integerValuesShouldMatchStringValuesInDatabase() {
		for (RequirementType type : RequirementType.values()) {
			Requirement current = requirementRepository.findOne(type.getValue());
			assertEquals(current.getCode(), type.name());
		}
	}
}
