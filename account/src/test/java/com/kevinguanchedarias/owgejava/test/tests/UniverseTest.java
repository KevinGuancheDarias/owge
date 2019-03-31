package com.kevinguanchedarias.owgejava.test.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kevinguanchedarias.owgejava.entity.Universe;
import com.kevinguanchedarias.owgejava.repository.UniverseRepository;

@ContextConfiguration(locations = { "file:src/test/resources/test-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UniverseTest extends TestCommon {

	@Autowired
	private UniverseRepository universeRepository;

	@Test
	public void shouldNotInsertUniverseDueToNullValues() {
		Universe universe = new Universe();
		checkNullSaveException(universeRepository, universe, null);
	}

	@Test
	public void shouldNotInsertUniverseDueToNullName() {
		Universe universe = prepareValidUniverse();
		universe.setName(null);
		checkNullSaveException(universeRepository, universe, "name");
	}

	@Test
	public void shouldNotInsertUniverseDueToNullCreationDate() {
		Universe universe = prepareValidUniverse();
		universe.setCreationDate(null);
		checkNullSaveException(universeRepository, universe, "creationDate");
	}

	@Test
	public void shouldNotInsertUniverseDueToNullPublic() {
		Universe universe = prepareValidUniverse();
		universe.setIsPublic(null);
		checkNullSaveException(universeRepository, universe, "isPublic");
	}

	@Test
	public void shouldNotInsertUniverseDueToNullOfficial() {
		Universe universe = prepareValidUniverse();
		universe.setOfficial(null);
		checkNullSaveException(universeRepository, universe, "official");
	}

	@Test
	public void shouldNotInsertUniverseDueToNullTargetDatabase() {
		Universe universe = prepareValidUniverse();
		universe.setTargetDatabase(null);
		checkNullSaveException(universeRepository, universe, "targetDatabase");
	}

	@Test
	public void shouldSave() {
		Universe universe = prepareValidUniverse();
		universeRepository.save(universe);
	}
}
