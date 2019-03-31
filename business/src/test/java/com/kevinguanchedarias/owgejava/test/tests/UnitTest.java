package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.UnitBo;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;

@ActiveProfiles("dev")
@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class UnitTest {
	@Autowired
	private UnitBo unitBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	private final Integer TEST_ID = 31;

	private Unit unit1;

	@PostConstruct
	public void init() {
		unit1 = new Unit();
		UnitType unitType = new UnitType();

		try {
			unitType.setName("soldiers");

			unitType = unitTypeBo.save(unitType);
		} catch (DataAccessException e) {
			System.out.println("Errors in UnitType module, please run UnitTypeTests");
			System.exit(1);
		}

		unit1.setName("Not Null!");
		unit1.setType(unitType);
	}

	@Test
	public void testUnitIsNull() {
		assertNull(unitBo.findById(TEST_ID));
	}

	@Test
	public void testUnitNotNull() {

		Unit persistent = unitBo.save(unit1);
		assertNotNull(unitBo.findById(persistent.getId()));
	}
}
