package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUniverseIsFull;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.Serial;
import java.util.List;

@Service
@AllArgsConstructor
public class PlanetBo implements WithNameBo<Long, Planet, PlanetDto> {
    private static final String PLANET_OWNED_CHANGE = "planet_owned_change";

    @Serial
    private static final long serialVersionUID = 3000986169771610777L;

    private final PlanetRepository planetRepository;
    private final UserStorageBo userStorageBo;
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

    public boolean canLeavePlanet(Integer invokerId, Long planetId) {
        return !isHomePlanet(planetId) && planetRepository.isOfUserProperty(invokerId, planetId)
                && !obtainedUnitRepository.hasUnitsInPlanet(invokerId, planetId)
                && missionFinderBo.findRunningUnitBuild(invokerId, (double) planetId) == null;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.14
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByGalaxy(Integer galaxyId) {
        planetRepository.deleteByGalaxyId(galaxyId);
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
        obtainedUnitRepository.findByUserIdAndTargetPlanetAndMissionTypeCode(owner.getId(), targetPlanet, MissionType.DEPLOYED.name())
                .forEach(units -> {
                    Mission mission = units.getMission();
                    obtainedUnitBo.moveUnit(units, owner.getId(), targetPlanet.getId());
                    if (mission != null) {
                        missionRepository.delete(mission);
                    }

                });
        if (targetPlanet.getSpecialLocation() != null) {
            requirementBo.triggerSpecialLocation(owner, targetPlanet.getSpecialLocation());
        }

        transactionUtilService.doAfterCommit(() -> planetListBo.emitByChangedPlanet(targetPlanet));
        emitPlanetOwnedChange(owner);
        missionEventEmitterBo.emitEnemyMissionsChange(owner);
        obtainedUnitEventEmitter.emitObtainedUnits(owner);
    }
}
