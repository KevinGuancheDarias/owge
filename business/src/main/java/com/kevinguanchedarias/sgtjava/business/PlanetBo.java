package com.kevinguanchedarias.sgtjava.business;

import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.sgtjava.entity.ExploredPlanet;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendUniverseIsFull;
import com.kevinguanchedarias.sgtjava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.sgtjava.repository.PlanetRepository;

@Component
public class PlanetBo implements WithNameBo<Planet> {
	private static final long serialVersionUID = 3000986169771610777L;

	@Autowired
	private PlanetRepository planetRepository;

	@Autowired
	private ExploredPlanetRepository exploredPlanetRepository;

	@Autowired
	private GalaxyBo galaxyBo;

	@Autowired
	private UserStorageBo userStorageBo;

	@Override
	public JpaRepository<Planet, Number> getRepository() {
		return planetRepository;
	}

	/**
	 * @param galaxyId
	 *            if null will be a random galaxy
	 * @return Random planet fom galaxy id
	 * @throws SgtBackendUniverseIsFull
	 * @author Kevin Guanche Darias
	 */
	public Planet findRandomPlanet(Integer galaxyId) {
		Integer targetGalaxy = galaxyId;
		if (targetGalaxy == null) {
			targetGalaxy = galaxyBo.findRandomGalaxy();
		}

		planetRepository.findAll();
		int count = (int) (planetRepository.countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(targetGalaxy));

		if (count == 0) {
			throw new SgtBackendUniverseIsFull("No hay más espacio en este universo");
		}

		int planetLocation = RandomUtils.nextInt(0, count);

		List<Planet> selectedPlanetsRange = planetRepository.findOneByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(
				targetGalaxy, new PageRequest(planetLocation, 1));

		return selectedPlanetsRange.get(0);
	}

	/**
	 * 
	 * @param user
	 *            the owner of the planets
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<Planet> findPlanetsByUser(UserStorage user) {
		return planetRepository.findByOwnerId(user.getId());
	}

	/**
	 * Find the planets for logged in user
	 * 
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<Planet> findMyPlanets() {
		return findPlanetsByUser(userStorageBo.findLoggedIn());
	}

	public List<Planet> findByGalaxyAndSectorAndQuadrant(Integer galaxy, Long sector, Long quadrant) {
		return planetRepository.findByGalaxyIdAndSectorAndQuadrant(galaxy, sector, quadrant);
	}

	public boolean isOfUserProperty(UserStorage expectedOwner, Planet planet) {
		return isOfUserProperty(expectedOwner.getId(), planet.getId());
	}

	public boolean isOfUserProperty(Integer expectedOwnerId, Long planetId) {
		return planetRepository.findOneByIdAndOwnerId(planetId, expectedOwnerId) != null;
	}

	public boolean myIsOfUserProperty(Planet planet) {
		return myIsExplored(planet.getId());
	}

	public boolean myIsOfUserProperty(Long planetId) {
		return isOfUserProperty(userStorageBo.findLoggedIn().getId(), planetId);
	}

	public boolean isExplored(UserStorage user, Planet planet) {
		return isExplored(user.getId(), planet.getId());
	}

	public boolean isExplored(Integer userId, Long planetId) {
		return isOfUserProperty(userId, planetId)
				|| exploredPlanetRepository.findOneByUserIdAndPlanetId(userId, planetId) != null;
	}

	public boolean myIsExplored(Planet planet) {
		return myIsExplored(planet.getId());
	}

	public boolean myIsExplored(Long planetId) {
		return isExplored(userStorageBo.findLoggedIn().getId(), planetId);
	}

	public void defineAsExplored(UserStorage user, Planet targetPlanet) {
		ExploredPlanet exploredPlanet = new ExploredPlanet();
		exploredPlanet.setUser(user);
		exploredPlanet.setPlanet(targetPlanet);
		exploredPlanetRepository.save(exploredPlanet);
	}

	public void myDefineAsExplored(Planet targetPlanet) {
		defineAsExplored(userStorageBo.findLoggedIn(), targetPlanet);
	}

}
