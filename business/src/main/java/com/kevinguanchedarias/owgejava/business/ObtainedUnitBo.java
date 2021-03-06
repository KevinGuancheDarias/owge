package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.kevinsuite.commons.exception.CommonException;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.OwgeElementSideDeletedException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.interfaces.ImprovementSource;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.util.ObtainedUnitUtil;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ObtainedUnitBo implements BaseBo<Long, ObtainedUnit, ObtainedUnitDto>, ImprovementSource {
    private static final long serialVersionUID = -2056602917496640872L;

    private final ObtainedUnitRepository repository;
    private final UserStorageBo userStorageBo;
    private final PlanetBo planetBo;
    private final UnitTypeBo unitTypeBo;
    private final ImprovementBo improvementBo;
    private final transient SocketIoService socketIoService;
    private final transient AsyncRunnerBo asyncRunnerBo;
    private final transient EntityManager entityManager;
    private final RequirementBo requirementBo;
    private final UnitMissionBo unitMissionBo;

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

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.interfaces.ImprovementSource#
     * calculateImprovement(com.kevinguanchedarias.owgejava.entity.UserStorage)
     */
    @Override
    public GroupedImprovement calculateImprovement(UserStorage user) {
        var groupedImprovement = new GroupedImprovement();
        findNotBuilding(user.getId()).forEach(current -> groupedImprovement.add(current.getUnit().getImprovement()));
        return groupedImprovement;
    }

    @PostConstruct
    public void init() {
        improvementBo.addImprovementSource(this);
    }

    /**
     * Finds all the user obtained units that are <b>not</b> in a building state
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<ObtainedUnit> findNotBuilding(Integer userId) {
        return repository.findByUserAndNotBuilding(userId);
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

    public List<ObtainedUnit> findByMissionId(Long missionId) {
        return repository.findByMissionId(missionId);
    }

    /**
     * @since 0.8.1
     */
    public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(Integer userId, Integer unitId,
                                                                                Long planetId) {
        return repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNull(userId, unitId, planetId);
    }


    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public ObtainedUnit findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(Integer userId, Integer unitId,
                                                                                  Long planetId) {
        return repository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndMissionTypeCode(userId, unitId, planetId,
                MissionType.DEPLOYED.name());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    public Long countByUserAndUnitType(UserStorage user, UnitType type) {
        return ObjectUtils.firstNonNull(repository.countByUserAndUnitType(user, type), 0L)
                + ObjectUtils.firstNonNull(repository.countByUserAndSharedCountUnitType(user, type), 0L);
    }

    /**
     * Returns the units in the <i>targetPlanet</i> that are not in mission <br>
     * Ideally used to explore a planet
     *
     * @param exploreMission mission that is executing the explore
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public List<ObtainedUnitDto> explorePlanetUnits(Mission exploreMission, Planet targetPlanet) {
        List<ObtainedUnitDto> retVal = toDto(repository.findByExplorePlanet(exploreMission.getId(), targetPlanet.getId()));
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
        ObtainedUnit existingOne = planetBo.isOfUserProperty(userId, targetPlanet)
                ? findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(userId, unitId, targetPlanet)
                : findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(userId, unitId, targetPlanet);
        if (existingOne == null) {
            retVal = save(obtainedUnit);
        } else {
            retVal = trySave(existingOne, obtainedUnit.getCount());
            if (obtainedUnit.getId() != null) {
                delete(obtainedUnit);
            }
        }
        return retVal;
    }

    @Transactional
    public void saveWithSubtraction(ObtainedUnitDto obtainedUnitDto, boolean handleImprovements) {
        ObtainedUnit unitBeforeDeletion = findByIdOrDie(obtainedUnitDto.getId());
        saveWithSubtraction(unitBeforeDeletion, obtainedUnitDto.getCount(),
                handleImprovements);
        Integer userId = obtainedUnitDto.getUserId();
        if (unitBeforeDeletion.getUnit().getEnergy() > 0) {
            userStorageBo.emitUserData(unitBeforeDeletion.getUser());
        }
        socketIoService.sendMessage(userId, "unit_type_change", () -> unitTypeBo.findUnitTypesWithUserInfo(userId));
        emitObtainedUnitChange(userId);
    }

    /**
     * Emits changes to elements affected by an unit alteration, for example, energy, or type's count
     */
    public void emitSideChanges(List<ObtainedUnit> obtainedUnits) {
        if (!obtainedUnits.isEmpty()) {
            UserStorage user = obtainedUnits.get(0).getUser();
            Integer userId = user.getId();

            if (obtainedUnits.stream().anyMatch(unit -> unit.getUnit().getEnergy() > 0)) {
                asyncRunnerBo.runAssyncWithoutContextDelayed(() -> userStorageBo.emitUserData(user));
            }
            asyncRunnerBo.runAssyncWithoutContextDelayed(() ->
                    socketIoService.sendMessage(userId, "unit_type_change", () -> unitTypeBo.findUnitTypesWithUserInfo(userId))
            );
            emitObtainedUnitChange(userId);
        }
    }

    @Override
    public void delete(List<ObtainedUnit> entities) {
        emitSideChanges(entities);
        BaseBo.super.delete(entities);
    }

    /**
     * Emits changed obtained units
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void emitObtainedUnitChange(Integer userId) {
        asyncRunnerBo.runAssyncWithoutContextDelayed(() -> socketIoService.sendMessage(userId,
                AbstractMissionBo.UNIT_OBTAINED_CHANGE, () -> toDto(findDeployedInUserOwnedPlanets(userId))), 500);
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
            TransactionUtil.doAfterCommit(() -> improvementBo.clearSourceCache(obtainedUnit.getUser(), this));
        }
        if (substractionCount < 0) {
            throw new SgtBackendInvalidInputException(
                    "Dear hacker, while this was possible in <= v0.9.13 , it's not now, you can go cry if you want");
        } else if (substractionCount > obtainedUnit.getCount()) {
            throw new SgtBackendInvalidInputException(
                    "Can't not subtract because, obtainedUnit count is less than the amount to subtract");
        } else if (obtainedUnit.getCount() > substractionCount) {
            requirementBo.triggerUnitBuildCompletedOrKilled(obtainedUnit.getUser(), obtainedUnit.getUnit());
            return trySave(obtainedUnit, -substractionCount);
        } else if (obtainedUnit.getCount().equals(substractionCount)) {
            delete(obtainedUnit);
            requirementBo.triggerUnitBuildCompletedOrKilled(obtainedUnit.getUser(), obtainedUnit.getUnit());
            return null;
        } else {
            throw new ProgrammingException("Should never ever happend");
        }
    }

    /**
     * Tries to save the new sumValue, if fails, will try till it success <br>
     * This strategy can be used to avoid to lock table rows
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.19
     */
    public ObtainedUnit trySave(ObtainedUnit obtainedUnit, long sumValue) {
        long expectedSum = obtainedUnit.getCount() + sumValue;
        obtainedUnit.setCount(expectedSum);
        ObtainedUnit savedValue = saveAndFlush(obtainedUnit);
        try {
            entityManager.refresh(obtainedUnit);
            if (!savedValue.getCount().equals(expectedSum)) {
                try {
                    Thread.sleep(RandomUtils.nextLong(0, 300));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CommonException("trySave failed for surprising reasons", e);
                }
                return trySave(savedValue, sumValue);
            } else {
                return savedValue;
            }
        } catch (EntityNotFoundException e) {
            throw new OwgeElementSideDeletedException("Element " + obtainedUnit.getClass().getName() + " with id "
                    + obtainedUnit.getId() + " has been side-deleted");
        }
    }

    /**
     * Deletes obtained units involved in passed mission <br>
     * <b>NOTICE: </b> By default will subtract improvements
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByMissionId(Long missionId) {
        repository.deleteByMissionId(missionId);
        improvementBo.clearCacheEntries(this);
    }

    /**
     * Deletes obtained units involved in passed mission <br>
     * <b>NOTICE: </b> As of 0.7.3, the param <i>subtractImprovements</i> can be
     * specified, for example to avoid removing improvements of a unit that has not
     * been even built
     *
     * @param subtractImprovements If true will too subtract improvements
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.3
     * @deprecated Use the version without the <i>subtractImprovements</i> param, as
     * it's not longer required
     */
    @Deprecated(since = "0.8.1")
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteByMissionId(Long missionId, boolean subtractImprovements) {
        int retVal = repository.findByMissionId(missionId).stream().map(current -> {
            delete(current);
            return current;
        }).collect(Collectors.toList()).size();
        if (subtractImprovements) {
            improvementBo.clearCacheEntries(this);

        }
        return retVal;
    }

    public List<ObtainedUnit> findInMyPlanet(Long planetId) {
        userStorageBo.checkOwnPlanet(planetId);
        return repository.findBySourcePlanetIdAndMissionNull(planetId);
    }

    /**
     * Finds all units that belong to given user and are not involved in any mission
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<ObtainedUnit> findDeployedInUserOwnedPlanets(Integer userId) {
        return repository.findBySourcePlanetNotNullAndMissionNullAndUserId(userId);
    }

    public List<ObtainedUnit> findInPlanetOrInMissiontoPlanet(Planet planet) {
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
        retVal.addAll(findInPlanetOrInMissiontoPlanet(attackedPlanet));
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
        var planet = planetBo.findById(planetId);
        ObtainedUnit savedUnit;
        unit.setSourcePlanet(unit.getSourcePlanet());
        unit.setTargetPlanet(planet);
        if (planetBo.isOfUserProperty(userId, planetId)) {
            unit.setSourcePlanet(planet);
            savedUnit = saveWithAdding(userId, unit, planetId);
            unit.setMission(null);
            unit.setTargetPlanet(null);
            unit.setFirstDeploymentMission(null);
        } else if (MissionType.valueOf(unit.getMission().getType().getCode()) == MissionType.DEPLOYED) {
            savedUnit = save(unit);
        } else {
            unit = saveWithAdding(userId, unit, planetId);
            savedUnit = unit;
            if (MissionType.valueOf(unit.getMission().getType().getCode()) != MissionType.DEPLOYED) {
                unit.setMission(unitMissionBo.findDeployedMissionOrCreate(unit));
                savedUnit = save(unit);
            }
        }
        return savedUnit;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.1
     */
    public List<ObtainedUnit> findByMissionIn(List<Long> missionIds) {
        return repository.findByMissionIdIn(missionIds);
    }

    public Double findConsumeEnergyByUser(UserStorage user) {
        return ObjectUtils.firstNonNull(repository.computeConsumedEnergyByUser(user), 0D);
    }

    /**
     * @param count Count to test if would exceed the unit type limit
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public boolean wouldReachUnitTypeLimit(UserStorage user, Integer typeId, Long count) {
        var faction = user.getFaction();
        var type = unitTypeBo.findById(typeId);
        var targetToCountTo = findMaxShareCountRoot(type);
        var userCount = ObjectUtils.firstNonNull(countByUserAndUnitType(user, targetToCountTo), 0L) + count;
        return unitTypeBo.hasMaxCount(faction, targetToCountTo)
                && userCount > unitTypeBo.findUniTypeLimitByUser(user, targetToCountTo.getId());
    }

    /**
     * Checks if the specified count would be over the expected count
     *
     * @throws SgtBackendInvalidInputException When reached
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void checkWouldReachUnitTypeLimit(UserStorage user, Integer typeId, Long count) {
        if (wouldReachUnitTypeLimit(user, typeId, count)) {
            throw new SgtBackendInvalidInputException(
                    "Nice try to buy over your possibilities!!!, try outside of Spain!");
        }
    }

    /**
     * Gets the ENUM value of the unit mission type (or null )
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    public MissionType resolveMissionType(ObtainedUnit unit) {
        if (unit.getMission() != null) {
            return MissionType.valueOf(unit.getMission().getType().getCode());
        } else {
            return null;
        }
    }

    private UnitType findMaxShareCountRoot(UnitType type) {
        return type.getShareMaxCount() == null ? type : findMaxShareCountRoot(type.getShareMaxCount());
    }

    /**
     *
     */
    public List<ObtainedUnit> findByUserIdAndTargetPlanetAndMissionTypeCode(Integer userId, Planet targetPlanet,
                                                                            MissionType missionType) {
        return repository.findByUserIdAndTargetPlanetAndMissionTypeCode(userId, targetPlanet, missionType.name());
    }
}
