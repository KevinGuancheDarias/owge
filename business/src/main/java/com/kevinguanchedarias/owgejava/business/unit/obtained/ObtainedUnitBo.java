package com.kevinguanchedarias.owgejava.business.unit.obtained;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.ObtainedUnitUtil;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ObtainedUnitBo implements BaseBo<Long, ObtainedUnit, ObtainedUnitDto> {

    @Serial
    private static final long serialVersionUID = -2056602917496640872L;

    private final ObtainedUnitRepository repository;
    private final ImprovementBo improvementBo;
    private final transient EntityManager entityManager;
    private final RequirementBo requirementBo;
    private final transient MissionFinderBo missionFinderBo;
    private final transient HiddenUnitBo hiddenUnitBo;
    private final transient ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final PlanetRepository planetRepository;
    private final transient TransactionUtilService transactionUtilService;
    private final transient ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;
    private final transient UserEventEmitterBo userEventEmitterBo;
    private final UnitTypeBo unitTypeBo;
    private final UserStorageRepository userStorageRepository;

    @Override
    public JpaRepository<ObtainedUnit, Long> getRepository() {
        return repository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<ObtainedUnitDto> getDtoClass() {
        return ObtainedUnitDto.class;
    }

    /**
     * Note: Takes into account also the units involved in missions that originate
     * from this planet, even if they are outside now
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public boolean hasUnitsInPlanet(Integer userId, Long planetId) {
        return repository.countByUserIdAndSourcePlanetIdAndMissionIsNull(userId, planetId) > 0;
    }

    /**
     * @since 0.8.1
     */
    public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(Integer userId, Integer unitId,
                                                                                Long planetId) {
        return repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(userId, unitId, planetId);
    }


    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    public ObtainedUnit findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(Integer userId, Integer unitId,
                                                                                  Long planetId) {
        return repository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(userId, unitId, planetId,
                MissionType.DEPLOYED.name());
    }

    /**
     * Returns the units in the <i>targetPlanet</i> that are not in mission <br>
     * Ideally used to explore a planet
     *
     * @param exploreMission mission that is executing the explore
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public List<ObtainedUnitDto> explorePlanetUnits(Mission exploreMission, Planet targetPlanet) {
        List<ObtainedUnit> entities = repository.findByExplorePlanet(exploreMission.getId(), targetPlanet.getId());
        List<ObtainedUnitDto> retVal = toDto(entities);

        hiddenUnitBo.defineHidden(entities, retVal);
        ObtainedUnitUtil.handleInvisible(retVal);
        return retVal;
    }

    /**
     * Saves the obtained unit to the database <br>
     * <b>IMPORTANT:</b> it may change the id, use the resultant value <br>
     * Will add the count to existing one, <b>if it exists in the planet</b>
     *
     * @param obtainedUnit <b>NOTICE:</b> Won't be changed from inside
     * @param targetPlanet Planet to where you are adding the units
     * @return new instance of saved obtained unit
     * @author Kevin Guanche Darias
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public ObtainedUnit saveWithAdding(Integer userId, ObtainedUnit obtainedUnit, Long targetPlanet) {
        Integer unitId = obtainedUnit.getUnit().getId();
        ObtainedUnit retVal;
        ObtainedUnit existingOne;
        var isOfUserProperty = planetRepository.isOfUserProperty(userId, targetPlanet);
        if (obtainedUnit.getExpirationId() == null) {
            existingOne = isOfUserProperty
                    ? findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(userId, unitId, targetPlanet)
                    : findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(userId, unitId, targetPlanet);
        } else {
            var expirationId = obtainedUnit.getExpirationId();
            existingOne = isOfUserProperty
                    ? repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNullAndExpirationId(userId, unitId, targetPlanet, expirationId)
                    : repository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndMissionTypeCodeAndExpirationId(userId, unitId, targetPlanet, MissionType.DEPLOYED.name(), expirationId);
        }
        if (existingOne == null) {
            retVal = repository.save(obtainedUnit);
        } else {
            retVal = saveWithChange(existingOne, obtainedUnit.getCount());
            if (obtainedUnit.getId() != null) {
                repository.delete(obtainedUnit);
            }
        }
        return retVal;
    }

    public void delete(List<ObtainedUnit> entities) {
        obtainedUnitEventEmitter.emitSideChanges(entities);
        repository.deleteAll(entities);
    }

    /**
     * Saves the Obtained unit with subtraction
     *
     * @param obtainedUnit       Target obtained unit
     * @param substractionCount  Count to subtract
     * @param handleImprovements If specified will sustract the improvements too
     * @return saved obtained unit, null if the count is the same
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional
    public ObtainedUnit saveWithSubtraction(ObtainedUnit obtainedUnit, Long substractionCount,
                                            boolean handleImprovements) {
        if (handleImprovements) {
            transactionUtilService.doAfterCommit(() -> improvementBo.clearSourceCache(obtainedUnit.getUser(), obtainedUnitImprovementCalculationService));
        }
        if (substractionCount < 0) {
            throw new SgtBackendInvalidInputException(
                    "Dear hacker, while this was possible in <= v0.9.13 , it's not now, you can go cry if you want");
        } else if (substractionCount > obtainedUnit.getCount()) {
            throw new SgtBackendInvalidInputException(
                    "Can't not subtract because, obtainedUnit count is less than the amount to subtract");
        } else if (obtainedUnit.getCount() > substractionCount) {
            requirementBo.triggerUnitBuildCompletedOrKilled(obtainedUnit.getUser(), obtainedUnit.getUnit());
            return saveWithChange(obtainedUnit, -substractionCount);
        } else if (obtainedUnit.getCount().equals(substractionCount)) {
            repository.delete(obtainedUnit);
            requirementBo.triggerUnitBuildCompletedOrKilled(obtainedUnit.getUser(), obtainedUnit.getUnit());
            return null;
        } else {
            throw new ProgrammingException("Should never ever happend");
        }
    }

    @Transactional
    public void saveWithSubtraction(ObtainedUnitDto obtainedUnitDto, boolean handleImprovements) {
        ObtainedUnit unitBeforeDeletion = findByIdOrDie(obtainedUnitDto.getId());
        saveWithSubtraction(unitBeforeDeletion, obtainedUnitDto.getCount(),
                handleImprovements);
        Integer userId = obtainedUnitDto.getUserId();
        if (unitBeforeDeletion.getUnit().getEnergy() > 0) {
            userEventEmitterBo.emitUserData(unitBeforeDeletion.getUser());
        }
        unitTypeBo.emitUserChange(userId);
        obtainedUnitEventEmitter.emitObtainedUnits(userStorageRepository.getById(userId));
    }

    public ObtainedUnit saveWithChange(ObtainedUnit obtainedUnit, long sumValue) {
        repository.updateCount(obtainedUnit, sumValue);
        entityManager.refresh(obtainedUnit);
        return obtainedUnit;
    }

    public List<ObtainedUnit> findInPlanetOrInMissionToPlanet(Planet planet) {
        List<ObtainedUnit> retVal = new ArrayList<>();
        retVal.addAll(repository.findBySourcePlanetIdAndMissionIsNull(planet.getId()));
        retVal.addAll(repository.findByTargetPlanetIdAndMissionTypeCode(planet.getId(),
                MissionType.DEPLOYED.toString()));
        return retVal;
    }

    /**
     * Finds the involved units in an attack
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public List<ObtainedUnit> findInvolvedInAttack(Planet attackedPlanet) {
        List<ObtainedUnit> retVal = new ArrayList<>();
        List<String> allowedMissions = new ArrayList<>();
        allowedMissions.add(MissionType.CONQUEST.name());
        retVal.addAll(findInPlanetOrInMissionToPlanet(attackedPlanet));
        retVal.addAll(repository.findByTargetPlanetIdWhereReferencePercentageTimePassed(attackedPlanet.getId(), 0.1d,
                allowedMissions, new Date()));
        return retVal;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.5
     */
    public boolean areUnitsInvolved(UserStorage user, Planet relatedPlanet) {
        return repository.areUnitsInvolved(user.getId(), user.getAlliance(), relatedPlanet.getId());
    }


    public boolean existsByMission(Mission mission) {
        return repository.countByMission(mission) > 0;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ObtainedUnit moveUnit(ObtainedUnit unit, Integer userId, Long planetId) {
        var planet = SpringRepositoryUtil.findByIdOrDie(planetRepository, planetId);
        ObtainedUnit savedUnit;
        unit.setTargetPlanet(planet);
        if (planetRepository.isOfUserProperty(userId, planetId)) {
            unit.setSourcePlanet(planet);
            savedUnit = saveWithAdding(userId, unit, planetId);
            unit.setMission(null);
            unit.setTargetPlanet(null);
            unit.setFirstDeploymentMission(null);
        } else if (unit.getMission() != null && MissionType.valueOf(unit.getMission().getType().getCode()) == MissionType.DEPLOYED) {
            savedUnit = repository.save(unit);
        } else {
            unit = saveWithAdding(userId, unit, planetId);
            savedUnit = unit;
            if (unit.getMission() == null || MissionType.valueOf(unit.getMission().getType().getCode()) != MissionType.DEPLOYED) {
                var deployedMission = missionFinderBo.findDeployedMissionOrCreate(unit);
                unit.setMission(deployedMission);
                savedUnit = repository.save(unit);
            }
        }
        return savedUnit;
    }

    /**
     * Gets the ENUM value of the unit mission type (or null )
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     * @deprecated Use {@link com.kevinguanchedarias.owgejava.business.mission.MissionTypeBo#resolve(Mission)}
     */
    @Deprecated(since = "0.11.0")
    public MissionType resolveMissionType(ObtainedUnit unit) {
        if (unit.getMission() != null) {
            return MissionType.valueOf(unit.getMission().getType().getCode());
        } else {
            return null;
        }
    }

    /**
     * Checks if the input Unit <i>id</i> exists, and returns the associated
     * ObtainedUnit
     *
     * @param isDeployedMission If true will search for a deployed obtained unit,
     *                          else for an obtained unit with a <i>null<i> mission
     * @return the expected obtained id
     * @throws NotFoundException If obtainedUnit doesn't exists
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public ObtainedUnit findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(Integer userId, Integer unitId,
                                                                               Long planetId, Long expirationId, boolean isDeployedMission) {

        ObtainedUnit retVal;
        if (expirationId == null) {
            retVal = isDeployedMission
                    ? findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(userId, unitId, planetId)
                    : findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(userId, unitId, planetId);
        } else {
            retVal = isDeployedMission
                    ? repository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdAndMissionTypeCode(
                    userId, unitId, planetId, expirationId, MissionType.DEPLOYED.name())
                    : repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdAndMissionIsNull(userId, unitId, planetId, expirationId);
        }


        if (retVal == null) {
            throw new NotFoundException("No obtainedUnit for unit with id " + unitId + " was found in planet "
                    + planetId + ", nice try, dirty hacker!");
        }
        return retVal;
    }

    /**
     *
     */
    public List<ObtainedUnit> findByUserIdAndTargetPlanetAndMissionTypeCode(Integer userId, Planet targetPlanet,
                                                                            MissionType missionType) {
        return repository.findByUserIdAndTargetPlanetAndMissionTypeCode(userId, targetPlanet, missionType.name());
    }
}
