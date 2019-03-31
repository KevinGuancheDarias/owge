package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.repository.FactionRepository;

@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class FactionBoTest {

	@Autowired
	private FactionRepository factionRepository;

	@Autowired
	private FactionBo factionBo;

	@Test
	public void shouldReturnOnlyVisibleFactions() {
		int times = 4;
		Faction model = new Faction();
		model.setHidden(false);
		model.setMaxPlanets(1);
		saveMultiple(model, times);

		model.setHidden(true);
		saveMultiple(model, times);

		assertEquals(times, factionBo.findVisible(false).size());

	}

	@Test
	public void shouldProperlyTellExistsAndIsVisible() {
		int times = 4;
		Faction model = new Faction();
		model.setHidden(false);
		model.setMaxPlanets(1);
		saveMultiple(model, times);

		model.setHidden(true);
		saveMultiple(model, times);

		int visible = 0;
		for (Faction current : factionRepository.findAll()) {
			if (factionBo.existsAndIsVisible(current.getId())) {
				visible++;
			}
		}

		assertEquals(times, visible);
	}

	/**
	 * Will save multiple factions of model
	 * 
	 * @param model
	 * @param times
	 *            Number of times to save
	 * @author Kevin Guanche Darias
	 */
	private void saveMultiple(Faction model, int times) {
		for (int i = 0; i < times; i++) {
			Faction currentModel = new Faction();
			BeanUtils.copyProperties(model, currentModel);
			String name = currentModel.getName() + " " + i;
			currentModel.setName(name);
			factionRepository.save(currentModel);
		}
	}
}
