package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.entity.ExploredPlanet;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUniverseIsFull;
import com.kevinguanchedarias.owgejava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serial;
import java.util.List;

@Service
public class PlanetBo implements WithNameBo<Long, Planet, PlanetDto> {
    public static final String PLANET_CACHE_TAG = "planet";

    private static final String PLANET_OWNED_CHANGE = "planet_owned_change";

    @Serial
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
    private transient PlanetListBo planetListBo;

    @PersistenceContext
    private transient EntityManager entityManager;

    @Autowired
    private transient TransactionUtilService transactionUtilService;

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
     * @throws SgtBackendUniverseIsFull When universe is full
     * @author Kevin Guanche Darias
     */
    public Planet findRandomPlanet(Integer galaxyId) {

        int count = galaxyId != null
                ? (int) (planetRepository.countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(galaxyId))
                : (int) (planetRepository.countByOwnerIsNullAndSpecialLocationIsNull());

        if (count == 0) {
            throw new SgtBackendUniverseIsFull("No hay más espacio en este universo");
        }

        int planetLocation = RandomUtils.nextInt(0, count);

        List<Planet> selectedPlanetsRange = galaxyId != null
                ? planetRepository.findOneByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(galaxyId,
                PageRequest.of(planetLocation, 1))
                : planetRepository.findOneByOwnerIsNullAndSpecialLocationIsNull(PageRequest.of(planetLocation, 1));

        return selectedPlanetsRange.get(0);
    }

    public List<Planet> findByGalaxyAndSectorAndQuadrant(Integer galaxy, Long sector, Long quadrant) {
        return planetRepository.findByGalaxyIdAndSectorAndQuadrant(galaxy, sector, quadrant);
    }

    /**
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public Planet findOneBySpecialLocationId(Integer specialLocationId) {
        return planetRepository.findOneBySpecialLocationId(specialLocationId);
    }

    public boolean isHomePlanet(Planet planet) {
        checkPersisted(planet);
        return planet.getHome() != null && planet.getHome();
    }

    public boolean isHomePlanet(Long planetId) {
        return planetRepository.findOneByIdAndHomeTrue(planetId) != null;
    }

    public boolean myIsOfUserProperty(Long planetId) {
        return planetRepository.isOfUserProperty(userStorageBo.findLoggedIn().getId(), planetId);
    }

    public void checkIsOfUserProperty(UserStorage user, Long planetId) {
        if (!planetRepository.isOfUserProperty(user.getId(), planetId)) {
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
        return planetRepository.isOfUserProperty(userId, planetId)
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

    /**
     * Checks if the user, has already the max planets he/she can have
     *
     * @return True, if the user has already the max planets he/she can have
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public boolean hasMaxPlanets(UserStorage user) {
        checkPersisted(user);
        int factionMax = user.getFaction().getMaxPlanets();
        int userPlanets = planetRepository.countByOwnerId(user.getId());
        return userPlanets >= factionMax;
    }

    @Transactional
    public void doLeavePlanet(Integer invokerId, Long planetId) {
        if (!canLeavePlanet(invokerId, planetId)) {
            throw new SgtBackendInvalidInputException("ERR_I18N_CAN_NOT_LEAVE_PLANET");
        }
        Planet planet = findById(planetId);
        UserStorage user = planet.getOwner();
        planet.setOwner(null);
        planetRepository.save(planet);
        if (planet.getSpecialLocation() != null) {
            requirementBo.triggerSpecialLocation(user, planet.getSpecialLocation());
        }
        transactionUtilService.doAfterCommit(() -> planetListBo.emitByChangedPlanet(planet));
        emitPlanetOwnedChange(invokerId);
    }

    /**
     * Emits the owned planet for the given target user
     */
    public void emitPlanetOwnedChange(UserStorage user) {
        emitPlanetOwnedChange(user.getId());
    }

    public void emitPlanetOwnedChange(Integer userId) {
        transactionUtilService.doAfterCommit(() -> socketIoService.sendMessage(userId, PLANET_OWNED_CHANGE,
                () -> toDto(planetRepository.findByOwnerId(userId))));
    }

    public boolean canLeavePlanet(UserStorage invoker, Planet planet) {
        return canLeavePlanet(invoker.getId(), planet.getId());
    }

    public boolean canLeavePlanet(Integer invokerId, Long planetId) {
        return !isHomePlanet(planetId) && planetRepository.isOfUserProperty(invokerId, planetId)
                && !obtainedUnitBo.hasUnitsInPlanet(invokerId, planetId)
                && missionBo.findRunningUnitBuild(invokerId, (double) planetId) == null;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @deprecated Due to Hibernate transactional auto save, can't modify the entity
     */
    @Deprecated(since = "0.9.12")
    public List<Planet> myCleanUpUnexplored(List<Planet> planets) {
        return cleanUpUnexplored(userStorageBo.findLoggedIn().getId(), planets);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @deprecated Due to Hibernate transactional auto save, can't modify the entity
     */
    @Deprecated(since = "0.9.13")
    public List<Planet> cleanUpUnexplored(Integer userId, List<Planet> planets) {
        planets.forEach(current -> {
            cleanUpUnexplored(userId, current);
        });
        return planets;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     * @deprecated Due to Hibernate transactional auto save, can't modify the entity
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.13
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.14
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByGalaxy(Integer galaxyId) {
        planetRepository.deleteByGalaxyId(galaxyId);
    }
}
