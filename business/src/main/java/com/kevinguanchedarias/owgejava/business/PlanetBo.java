package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUniverseIsFull;
import com.kevinguanchedarias.owgejava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PlanetBo implements WithNameBo<Long, Planet, PlanetDto>, UserDeleteListener {
    public static final String PLANET_OWNED_CHANGE = "planet_owned_change";
    public static final int USER_DELETE_ORDER = ObtainedUnitBo.OBTAINED_UNIT_USER_DELETE_ORDER + 1;

    @Serial
    private static final long serialVersionUID = 3000986169771610777L;

    private final PlanetRepository planetRepository;
    private final transient UserSessionService userSessionService;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final transient SocketIoService socketIoService;
    private final RequirementBo requirementBo;
    private final transient PlanetListBo planetListBo;
    private final transient EntityManager entityManager;
    private final transient TransactionUtilService transactionUtilService;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionRepository missionRepository;
    private final transient MissionEventEmitterBo missionEventEmitterBo;
    private final transient ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final transient MissionFinderBo missionFinderBo;
    private final DtoUtilService dtoUtilService;
    private final ExploredPlanetRepository exploredPlanetRepository;

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
     * <b>Notice:</b> Expensive method
     *
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

        var selectedPlanetsRange = galaxyId != null
                ? planetRepository.findByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(galaxyId,
                PageRequest.of(planetLocation, 1))
                : planetRepository.findByOwnerIsNullAndSpecialLocationIsNull(PageRequest.of(planetLocation, 1));

        return selectedPlanetsRange.get(0);
    }

    public List<Planet> findByGalaxyAndSectorAndQuadrant(Integer galaxy, Long sector, Long quadrant) {
        return planetRepository.findByGalaxyIdAndSectorAndQuadrant(galaxy, sector, quadrant);
    }

    public boolean isHomePlanet(Planet planet) {
        checkPersisted(planet);
        return planet.getHome() != null && planet.getHome();
    }

    public boolean isHomePlanet(Long planetId) {
        return planetRepository.findOneByIdAndHomeTrue(planetId) != null;
    }

    public boolean myIsOfUserProperty(Long planetId) {
        return planetRepository.isOfUserProperty(userSessionService.findLoggedIn().getId(), planetId);
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
        var planet = findById(planetId);
        var user = planet.getOwner();
        planet.setOwner(null);
        planetRepository.save(planet);
        maybeTriggerSpecialLocation(planet, user);
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

    public boolean canLeavePlanet(Integer invokerId, Long planetId) {
        return !isHomePlanet(planetId) && planetRepository.isOfUserProperty(invokerId, planetId)
                && !obtainedUnitRepository.hasUnitsInPlanet(invokerId, planetId)
                && missionFinderBo.findRunningUnitBuild(invokerId, (double) planetId) == null;
    }


    /**
     * Defines the new owner for the targetPlanet
     *
     * @param owner         The new owner
     * @param involvedUnits The units used by the owner to conquest the planet
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void definePlanetAsOwnedBy(UserStorage owner, List<ObtainedUnit> involvedUnits, Planet targetPlanet) {
        targetPlanet.setOwner(owner);
        involvedUnits.forEach(current -> {
            current.setSourcePlanet(targetPlanet);
            current.setTargetPlanet(null);
            current.setMission(null);
        });
        planetRepository.save(targetPlanet);
        var deployedUnits = obtainedUnitRepository
                .findByUserIdAndTargetPlanetAndMissionTypeCode(owner.getId(), targetPlanet, MissionType.DEPLOYED.name());
        var deployedMissions = deployedUnits.stream()
                .map(ObtainedUnit::getMission)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        deployedUnits.forEach(unit -> {
            // Detach before moveUnit: its lookup queries auto-flush this entity, and Hibernate
            // writes all columns, so a still-set mission would be re-asserted against the db
            unit.setMission(null);
            obtainedUnitBo.moveUnit(unit, owner.getId(), targetPlanet.getId());
        });
        // Deleting only after every unit is detached, else the flush of a later unit's update
        // references an already deleted mission, throwing FK violation 1452 (see mission 395869 dc12 incident)
        missionRepository.deleteAll(deployedMissions);
        maybeTriggerSpecialLocation(targetPlanet, owner);

        transactionUtilService.doAfterCommit(() -> planetListBo.emitByChangedPlanet(targetPlanet));
        emitPlanetOwnedChange(owner);
        missionEventEmitterBo.emitEnemyMissionsChange(owner);
        obtainedUnitEventEmitter.emitObtainedUnits(owner);
    }

    /**
     * Admin data repair: re-runs the HAVE_SPECIAL_LOCATION grant trigger for
     * every currently-owned special-location planet (see
     * rust-backend/docs/BUG-SPECIAL-LOCATION-UNLOCK.md "Consequences" — the
     * Rust backend historically never granted these unlocks on ownership
     * change). The requirement re-evaluation converges, so running it is
     * idempotent and safe universe-wide.
     *
     * @return the number of (planet, owner) pairs re-evaluated
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional
    public int repairSpecialLocationUnlocks() {
        var planets = planetRepository.findByOwnerNotNullAndSpecialLocationNotNullOrderById();
        planets.forEach(planet -> maybeTriggerSpecialLocation(planet, planet.getOwner()));
        return planets.size();
    }

    @Override
    public List<PlanetDto> toDto(List<Planet> entities) {
        return dtoUtilService.convertEntireArray(getDtoClass(), entities);
    }

    private void maybeTriggerSpecialLocation(Planet planet, UserStorage user) {
        if (planet.getSpecialLocation() != null) {
            requirementBo.triggerSpecialLocation(user, planet.getSpecialLocation());
        }
    }

    @Override
    public int order() {
        return USER_DELETE_ORDER;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        var homePlanet = user.getHomePlanet();
        homePlanet.setHome(false);
        planetRepository.save(homePlanet);
        planetRepository.nullifyGivenOwner(user);
        exploredPlanetRepository.deleteByUser(user);
    }
}
