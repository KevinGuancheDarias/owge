package com.kevinguanchedarias.owgejava.business;

import java.util.ArrayList;
import java.util.concurrent.Future;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNoGalaxiesFound;
import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;

@Service
public class GalaxyBo implements WithNameBo<Galaxy> {
	private static final long serialVersionUID = 5691936505840441041L;

	private static final Integer PLANETS_FOR_EACH_QUADRANT = 20;
	private static final Long GALAXY_MAX_LENGTH = 50000L;

	@Autowired
	private GalaxyRepository galaxyRepository;

	@Autowired
	private PlanetRepository planetRepository;

	@Override
	public JpaRepository<Galaxy, Number> getRepository() {
		return galaxyRepository;
	}

	/**
	 * Will async save the galaxy
	 * 
	 * @param galaxy
	 * @return - Saved Galaxy, or null if an error occurred
	 */
	@Async
	public Future<Galaxy> saveAsync(Galaxy galaxy) {
		canSave(galaxy);

		if (galaxy.getId() == null) {
			prepareGalaxy(galaxy);
		}

		return new AsyncResult<>(galaxyRepository.saveAndFlush(galaxy));
	}

	/**
	 * Will check if it's possible to save the galaxy
	 * 
	 * @param galaxy
	 * @author Kevin Guanche Darias
	 * @throws SgtBackendInvalidInputException
	 *             When it's not possible to save
	 */
	public void canSave(Galaxy galaxy) {
		checkInput(galaxy);
		checkUnused(galaxy);
	}

	/**
	 * Returns number of planets galaxy will have
	 * 
	 * @param galaxy
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public Long computedPlanetsCount(Galaxy galaxy) {
		return galaxy.getSectors() * galaxy.getQuadrants() * PLANETS_FOR_EACH_QUADRANT;
	}

	/**
	 * @return Returns a random galaxy
	 * @throws SgtBackendNoGalaxiesFound
	 * @author Kevin Guanche Darias
	 */
	public Integer findRandomGalaxy() {
		int count = countAll().intValue();

		if (count == 0) {
			throw new SgtBackendNoGalaxiesFound("Este universo no posee galaxias");
		}

		int selectedGalaxy = RandomUtils.nextInt(0, count);

		return galaxyRepository.findAll(new PageRequest(selectedGalaxy, 1)).getContent().get(0).getId();
	}

	private void checkInput(Galaxy galaxy) {
		if (galaxy.getSectors() < 1 || galaxy.getQuadrants() < 1) {
			throw new SgtBackendInvalidInputException("Datos de entrada no válidos");
		}

		if (computedPlanetsCount(galaxy) > GALAXY_MAX_LENGTH) {
			throw new SgtBackendInvalidInputException(
					"La galaxia no puede tener más de " + GALAXY_MAX_LENGTH + " planetas");
		}
	}

	/**
	 * Will check if the selected galaxy is empty Considered empty when there
	 * are not players in it
	 * 
	 * @param galaxy
	 * @author Kevin Guanche Darias
	 */
	private void checkUnused(Galaxy galaxy) {
		if (planetRepository.findOneByGalaxyIdAndOwnerNotNullOrderByGalaxyId(galaxy.getId()) != null) {
			throw new SgtBackendInvalidInputException("No se alterar una galaxia que ya contiene jugadores");
		}
	}

	/**
	 * Will append a transient planet instance to the galaxy
	 * 
	 * @param richnessPosibilities
	 * @param galaxy
	 * @param sector
	 * @param quadrant
	 * @param planetNumber
	 * @author Kevin Guanche Darias
	 */
	private void preparePlanet(Integer[] richnessPosibilities, Galaxy galaxy, int sector, int quadrant,
			int planetNumber) {
		Planet planet = new Planet();
		planet.setName(galaxy.getName().substring(0, 1) + "S" + sector + "C" + quadrant + "N" + planetNumber);
		planet.setRichness(richnessPosibilities[RandomUtils.nextInt(0, richnessPosibilities.length)]);
		planet.setGalaxy(galaxy);
		planet.setSector((long) sector);
		planet.setQuadrant((long) quadrant);
		planet.setPlanetNumber(planetNumber);

		if (galaxy.getPlanets() == null) {
			galaxy.setPlanets(new ArrayList<>());
		}

		galaxy.getPlanets().add(planet);
	}

	/**
	 * Prepares the galaxy for saving, so if galaxy has never been persisted
	 * will insert ALL its planets WARNING: HEAVY INTENSE OPERATION!
	 * 
	 * @param galaxy
	 * @author Kevin Guanche Darias
	 */
	private void prepareGalaxy(Galaxy galaxy) {
		Integer[] richnessPosibilities = generateRichnessPosibilities();

		for (int sector = 1; sector <= galaxy.getSectors(); sector++) {
			for (int quadrant = 1; quadrant <= galaxy.getQuadrants(); quadrant++) {
				for (int planetNumber = 1; planetNumber <= PLANETS_FOR_EACH_QUADRANT; planetNumber++) {
					preparePlanet(richnessPosibilities, galaxy, sector, quadrant, planetNumber);
				}
			}
		}
	}

	/**
	 * Will generate the richness possibilities
	 * 
	 * @return
	 */
	private Integer[] generateRichnessPosibilities() {
		return new Integer[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
	}
}
