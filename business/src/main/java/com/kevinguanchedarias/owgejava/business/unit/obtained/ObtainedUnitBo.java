package com.kevinguanchedarias.owgejava.business.unit.obtained;

import com.kevinguanchedarias.owgejava.business.*;
import com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.util.EntityRefreshUtilService;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.hotfix.ObtainedUnitHotFixRepository;
import com.kevinguanchedarias.owgejava.util.ObtainedUnitUtil;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheEvictByTag;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ObtainedUnitBo implements BaseBo<Long, ObtainedUnit, ObtainedUnitDto>, UserDeleteListener {
    public static final int OBTAINED_UNIT_USER_DELETE_ORDER = UnitMissionBo.UNIT_MISSION_USER_DELETE_ORDER - 1;

    @Serial
    private static final long serialVersionUID = -2056602917496640872L;

    private final ObtainedUnitRepository repository;
    private final ImprovementBo improvementBo;
    private final transient EntityRefreshUtilService entityRefreshUtilService;
    private final RequirementBo requirementBo;
    private final transient MissionFinderBo missionFinderBo;
    private final transient HiddenUnitBo hiddenUnitBo;
    private final transient ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final PlanetRepository planetRepository;
    private final transient TransactionUtilService transactionUtilService;
    private final transient ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;
    private final transient UserEventEmitterBo userEventEmitterBo;
    private final UnitTypeBo unitTypeBo;
    private final transient TaggableCacheManager taggableCacheManager;
    private final ObtainedUnitHotFixRepository obtainedUnitHotFixRepository;

    @Override
    public JpaRepository<ObtainedUnit, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<ObtainedUnitDto> getDtoClass() {
        return ObtainedUnitDto.class;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    public ObtainedUnit findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(Integer userId, Integer unitId,
                                                                                  Long planetId) {
        return obtainedUnitHotFixRepository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(
                userId, unitId, planetId, MissionType.DEPLOYED.name()
        );
    }

    /**
     * Returns the units in the <i>targetPlanet</i> that are not in mission <br>
     * Ideally used to explore a planet
     *
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
                    ? obtainedUnitHotFixRepository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(
                    userId, unitId, targetPlanet)
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
     * @param subtractionCount   Count to subtract
     * @param handleImprovements If specified will subtract the improvements too
     * @return saved obtained unit, null if the count is the same
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional
    @TaggableCacheEvictByTag(tags = ObtainedUnit.OBTAINED_UNIT_CACHE_TAG_BY_USER + ":#obtainedUnit.user.id")
    public ObtainedUnit saveWithSubtraction(ObtainedUnit obtainedUnit, Long subtractionCount,
                                            boolean handleImprovements) {
        if (handleImprovements) {
            transactionUtilService.doAfterCommit(() -> improvementBo.clearSourceCache(obtainedUnit.getUser(), obtainedUnitImprovementCalculationService));
        }
        if (subtractionCount < 0) {
            throw new SgtBackendInvalidInputException(
                    "Dear hacker, while this was possible in <= v0.9.13 , it's not now, you can go cry if you want");
        } else if (subtractionCount > obtainedUnit.getCount()) {
            throw new SgtBackendInvalidInputException(
                    "Can't not subtract because, obtainedUnit count is less than the amount to subtract");
        } else if (subtractionCount < obtainedUnit.getCount()) {
            requirementBo.triggerUnitBuildCompletedOrKilled(obtainedUnit.getUser(), obtainedUnit.getUnit());
            return saveWithChange(obtainedUnit, -subtractionCount);
        } else {
            repository.delete(obtainedUnit);
            requirementBo.triggerUnitBuildCompletedOrKilled(obtainedUnit.getUser(), obtainedUnit.getUnit());
        }
        return null;
    }

    @Transactional
    @TaggableCacheEvictByTag(tags = ObtainedUnit.OBTAINED_UNIT_CACHE_TAG_BY_USER + ":#obtainedUnitDto.userId")
    public void saveWithSubtraction(ObtainedUnitDto obtainedUnitDto, boolean handleImprovements) {
        var unitBeforeDeletion = findByIdOrDie(obtainedUnitDto.getId());
        saveWithSubtraction(unitBeforeDeletion, obtainedUnitDto.getCount(),
                handleImprovements);
        var userId = obtainedUnitDto.getUserId();
        if (unitBeforeDeletion.getUnit().getEnergy() > 0) {
            userEventEmitterBo.emitUserData(unitBeforeDeletion.getUser());
        }
        unitTypeBo.emitUserChange(userId);
        obtainedUnitEventEmitter.emitObtainedUnits(unitBeforeDeletion.getUser());
    }

    /**
     * Updates the count,
     * <b>Notice:</b> due to spring jpa repository Modifying not triggering the {@link com.kevinguanchedarias.owgejava.entity.listener.EntityWithByUserCacheTagListener}
     * Will have to manually drop cache tag
     */
    public ObtainedUnit saveWithChange(ObtainedUnit obtainedUnit, long sumValue) {
        repository.updateCount(obtainedUnit, sumValue);
        obtainedUnit = entityRefreshUtilService.refresh(obtainedUnit);
        taggableCacheManager.evictByCacheTag(obtainedUnit.getByUserCacheTag(), obtainedUnit.getUser().getId());
        return obtainedUnit;
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
            unit.setOwnerUnit(null);
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
     * Checks if the input Unit <i>id</i> exists, and returns the associated
     * ObtainedUnit
     *
     * @param isDeployedMission If true will search for a deployed obtained unit,
     *                          else for an obtained unit with a <i>null<i> mission
     * @return the expected obtained id
     * @throws NotFoundException If obtainedUnit doesn't exist
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public ObtainedUnit findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(Integer userId, Integer unitId,
                                                                               Long planetId, Long expirationId, boolean isDeployedMission) {
        ObtainedUnit retVal;
        if (expirationId == null) {
            retVal = isDeployedMission
                    ? findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(userId, unitId, planetId)
                    : obtainedUnitHotFixRepository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(
                    userId, unitId, planetId);
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

    @Override
    public int order() {
        return OBTAINED_UNIT_USER_DELETE_ORDER;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        repository.deleteByUser(user);
    }
}
