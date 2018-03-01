package com.kevinguanchedariassgtjava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kevinguanchedarias.sgtjava.business.UniverseBo;
import com.kevinguanchedarias.sgtjava.entity.Universe;

@ContextConfiguration(locations = { "file:src/test/resources/test-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UniverseBoTest extends TestCommon {

	@Autowired
	private UniverseBo universeBo;

	@Test
	public void shouldReturnOnlyOfficials() {
		int times = 7;
		Universe universe = prepareValidUniverse();
		universe.setOfficial(false);
		persistMultipleUniverses(universe, times);

		universe.setOfficial(true);
		persistMultipleUniverses(universe, times);

		assertEquals(times * 2, universeBo.findAll().size());

		List<Universe> retVal = universeBo.findOfficialUniverses();
		assertTrue(retVal.get(0).getOfficial());
		assertEquals(times, retVal.size());
	}
}
