package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.business.UnitTypeBo;
import com.kevinguanchedarias.sgtjava.entity.UnitType;

@ActiveProfiles("dev")
@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class UnitTypeTest {

	@Autowired
	private UnitTypeBo unitTypeBo;
	// private final String UNIT_TYPES_TABLE = "unit_types";
	private final Integer TEST_ID = 74;

	private UnitType unitType1 = new UnitType(TEST_ID, "Tropas", null);

	@Test
	public void testUnitTypeIsNull() {
		assertNull(unitTypeBo.findById(TEST_ID));
	}

	@Test
	public void testUnitTypeNotNull() {

		UnitType persistent = unitTypeBo.save(unitType1);
		assertNotNull(unitTypeBo.findById(persistent.getId()));
	}
}