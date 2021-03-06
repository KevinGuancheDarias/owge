package com.kevinguanchedarias.owgejava.business;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.entity.ExploredPlanet;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUniverseIsFull;
import com.kevinguanchedarias.owgejava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;

@Component
public class PlanetBo implements WithNameBo<Long, Planet, PlanetDto> {
	private static final String PLANET_OWNED_CHANGE = "planet_owned_change";

	private static final long serialVersionUID = 3000986169771610777L;

	@Autowired
	private PlanetRepository planetRepository;

	@Autowired
	private ExploredPlanetRepository exploredPlanetRepository;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private MissionBo missionBo;

	@Autowired
	private transient SocketIoService socketIoService;

	@Autowired
	@Lazy
	private RequirementBo requirementBo;

	@Autowired
	private PlanetListBo planetListBo;

	@PersistenceContext
	private transient EntityManager entityManager;

	@Override
	public JpaRepository<Planet, Long> getRepository() {
		return planetRepository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<PlanetDto> getDtoClass() {
		return PlanetDto.class;
	}

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	/**
	 * @param galaxyId if null will be a random galaxy
	 * @return Random planet fom galaxy id
	 * @throws SgtBackendUniverseIsFull
	 * @author Kevin Guanche Darias
	 */
	public Planet findRandomPlanet(Integer galaxyId) {
		Integer targetGalaxy = galaxyId;

		int count = galaxyId != null
				? (int) (planetRepository.countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(galaxyId))
				: (int) (planetRepository.countByOwnerIsNullAndSpecialLocationIsNull());

		if (count == 0) {
			throw new SgtBackendUniverseIsFull("No hay más espacio en este universo");
		}

		int planetLocation = RandomUtils.nextInt(0, count);

		List<Planet> selectedPlanetsRange = galaxyId != null
				? planetRepository.findOneByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(targetGalaxy,
						PageRequest.of(planetLocation, 1))
				: planetRepository.findOneByOwnerIsNullAndSpecialLocationIsNull(PageRequest.of(planetLocation, 1));

		return selectedPlanetsRange.get(0);
	}

	/**
	 *
	 * @param user the owner of the planets
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<Planet> findPlanetsByUser(UserStorage user) {
		return planetRepository.findByOwnerId(user.getId());
	}

	/**
	 * Finds all the planets that has owner in the specified galaxy id
	 *
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @param galaxyId
	 * @return
	 */
	public List<Planet> findByGalaxyIdAndOwnerNotNull(Integer galaxyId) {
		return planetRepository.findByGalaxyIdAndOwnerNotNull(galaxyId);
	}

	public List<Planet> findByGalaxyAndSectorAndQuadrant(Integer galaxy, Long sector, Long quadrant) {
		return planetRepository.findByGalaxyIdAndSectorAndQuadrant(galaxy, sector, quadrant);
	}

	/**
	 *
	 *
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @param specialLocationId
	 * @return
	 */
	public Planet findOneBySpecialLocationId(Integer specialLocationId) {
		return planetRepository.findOneBySpecialLocationId(specialLocationId);
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

	public boolean isHomePlanet(Planet planet) {
		checkPersisted(planet);
		return planet.getHome() != null && planet.getHome();
	}

	public boolean isHomePlanet(Long planetId) {
		return planetRepository.findOneByIdAndHomeTrue(planetId) != null;
	}

	public boolean myIsOfUserProperty(Long planetId) {
		return isOfUserProperty(userStorageBo.findLoggedIn().getId(), planetId);
	}

	public void checkIsOfUserProperty(UserStorage user, Long planetId) {
		if (!isOfUserProperty(user.getId(), planetId)) {
			throw new SgtBackendInvalidInputException(
					"Specified planet with id " + planetId + " does NOT belong to the user");
		}
	}

	public void myCheckIsOfUserProperty(Long planetId) {
		checkIsOfUserProperty(userStorageBo.findLoggedIn(), planetId);
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
		socketIoService.sendMessage(user, "planet_explored_event", () -> toDto(findById(targetPlanet.getId())));
	}

	public void myDefineAsExplored(Planet targetPlanet) {
		defineAsExplored(userStorageBo.findLoggedIn(), targetPlanet);
	}

	/**
	 * Checks if the user, has already the max planets he/she can have
	 *
	 * @param user
	 * @return True, if the user has already the max planets he/she can have
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean hasMaxPlanets(UserStorage user) {
		checkPersisted(user);
		int factionMax = user.getFaction().getMaxPlanets();
		int userPlanets = planetRepository.countByOwnerId(user.getId());
		return userPlanets >= factionMax;
	}

	public boolean hasMaxPlanets(Integer userId) {
		return hasMaxPlanets(userStorageBo.findById(userId));
	}

	@Transactional
	public void doLeavePlanet(Integer invokerId, Long planetId) {
		if (!canLeavePlanet(invokerId, planetId)) {
			throw new SgtBackendInvalidInputException("ERR_I18N_CAN_NOT_LEAVE_PLANET");
		}
		Planet planet = findById(planetId);
		UserStorage user = planet.getOwner();
		planet.setOwner(null);
		save(planet);
		if (planet.getSpecialLocation() != null) {
			requirementBo.triggerSpecialLocation(user, planet.getSpecialLocation());
		}
		TransactionUtil.doAfterCommit(() -> planetListBo.emitByChangedPlanet(planet));
		emitPlanetOwnedChange(invokerId);
	}

	/**
	 * Emits the owned planet for the given target user
	 *
	 * @param user
	 */
	public void emitPlanetOwnedChange(UserStorage user) {
		emitPlanetOwnedChange(user.getId());
	}

	public void emitPlanetOwnedChange(Integer userId) {
		TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(userId, PLANET_OWNED_CHANGE,
				() -> toDto(findPlanetsByUser(userStorageBo.findById(userId)))));
	}

	public boolean canLeavePlanet(UserStorage invoker, Planet planet) {
		return canLeavePlanet(invoker.getId(), planet.getId());
	}

	public boolean canLeavePlanet(Integer invokerId, Long planetId) {
		return !isHomePlanet(planetId) && isOfUserProperty(invokerId, planetId)
				&& !obtainedUnitBo.hasUnitsInPlanet(invokerId, planetId)
				&& missionBo.findRunningUnitBuild(invokerId, (double) planetId) == null;
	}

	/**
	 *
	 * @param planets
	 * @return
	 * @since 0.9.0
	 * @deprecated Due to Hibernate transactional auto save, can't modify the entity
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.9.12")
	public List<Planet> myCleanUpUnexplored(List<Planet> planets) {
		return cleanUpUnexplored(userStorageBo.findLoggedIn().getId(), planets);
	}

	/**
	 *
	 * @param userId
	 * @param planets
	 * @return
	 * @since 0.9.0
	 * @deprecated Due to Hibernate transactional auto save, can't modify the entity
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.9.13")
	public List<Planet> cleanUpUnexplored(Integer userId, List<Planet> planets) {
		planets.forEach(current -> {
			cleanUpUnexplored(userId, current);
		});
		return planets;
	}

	/**
	 *
	 * @param userId
	 * @param planet
	 * @since 0.9.9
	 * @deprecated Due to Hibernate transactional auto save, can't modify the entity
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.9.13")
	public void cleanUpUnexplored(Integer userId, Planet planet) {
		if (!isExplored(userId, planet.getId())) {
			planet.setName(null);
			planet.setRichness(null);
			planet.setHome(null);
			planet.setOwner(null);
			planet.setSpecialLocation(null);
		}
	}

	/**
	 *
	 * @param userId
	 * @param planetDto
	 * @since 0.9.13
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void cleanUpUnexplored(Integer userId, PlanetDto planetDto) {
		if (!isExplored(userId, planetDto.getId())) {
			planetDto.setName(null);
			planetDto.setRichness(null);
			planetDto.setHome(null);
			planetDto.setOwnerId(null);
			planetDto.setOwnerName(null);
			planetDto.setSpecialLocation(null);
		}
	}

	/**
	 *
	 * @param galaxyId
	 * @since 0.9.14
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void deleteByGalaxy(Integer galaxyId) {
		planetRepository.deleteByGalaxyId(galaxyId);
	}
}
