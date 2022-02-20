package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.requirement.RequirementSource;
import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.dto.UpgradeDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObjectRelationToObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Requirement;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.ObjectType;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.InvalidConfigurationException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.pojo.UnitUpgradeRequirements;
import com.kevinguanchedarias.owgejava.pojo.UnitWithRequirementInformation;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import com.kevinguanchedarias.owgejava.util.ValidationUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Transactional
public class RequirementBo implements Serializable {
    @Serial
    private static final long serialVersionUID = -7069590234333605969L;

    private static final Logger LOG = Logger.getLogger(RequirementBo.class);

    @Autowired
    private UserStorageBo userStorageBo;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private RequirementInformationDao requirementDao;

    @Autowired
    private UnlockedRelationBo unlockedRelationBo;

    @Autowired
    private ObtainedUpgradeBo obtainedUpgradeBo;

    @Autowired
    private UpgradeBo upgradeBo;

    @Autowired
    private ObjectRelationBo objectRelationBo;

    @Autowired
    private ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo;

    @Autowired
    private DtoUtilService dtoUtilService;

    @Autowired
    private RequirementInformationBo requirementInformationBo;

    @Autowired
    private transient AutowireCapableBeanFactory beanFactory;

    @Autowired
    private transient SocketIoService socketIoService;

    @Autowired
    private TimeSpecialBo timeSpecialBo;

    @Autowired
    private UnitBo unitBo;

    @Autowired
    private SpeedImpactGroupBo speedImpactGroupBo;

    @Autowired
    private PlanetBo planetBo;

    @Autowired
    private transient EntityManager entityManager;

    @Autowired
    private ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private List<RequirementSource> requirementSources;

    /**
     * Checks that the {@link RequirementTypeEnum} enum matches the database values
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @PostConstruct
    public void init() {
        Map<String, Requirement> requirementsMap = new HashMap<>();
        RequirementTypeEnum[] validValues = RequirementTypeEnum.values();
        try {
            findAll().forEach(current -> requirementsMap.put(current.getCode(), current));
            if (requirementsMap.size() != validValues.length) {
                throw new SgtCorruptDatabaseException("Database Stored values don't match  enum values");
            }
            Stream.of(validValues).forEachOrdered(current -> requirementsMap.get(current.name()));
        } catch (Exception e) {
            throw new InvalidConfigurationException(e);
        }
    }

    public List<Requirement> findAll() {
        return requirementRepository.findAll();
    }

    public Requirement findOneByCode(RequirementTypeEnum code) {
        return requirementRepository.findOneByCode(code.name());
    }

    /**
     * Will return requirement for specified object type with the given referenceId
     *
     * @param targetObject - Type of object
     * @param referenceId  - Id on the target entity, for example id of an upgrade,
     *                     or an unit
     * @author Kevin Guanche Darias
     * @deprecated Use {@link RequirementBo#findRequirements(ObjectEnum, Integer)}
     */
    @Deprecated(since = "0.8.0")
    public List<RequirementInformation> getRequirements(RequirementTargetObject targetObject, Integer referenceId) {
        return requirementDao.getRequirements(targetObject, referenceId);
    }

    /**
     * Will return requirement for specified object type with the given referenceId
     *
     * @param objectEnum  Type of object
     * @param referenceId Id on the target entity, for example id of an upgrade, or
     *                    an unit
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<RequirementInformation> findRequirements(ObjectEnum objectEnum, Integer referenceId) {
        return requirementDao.findRequirements(objectEnum, referenceId);
    }

    @Cacheable(cacheNames = "requirements_by_faction", key = "#faction.id")
    public List<UnitWithRequirementInformation> findFactionUnitLevelRequirements(Faction faction) {
        return requirementDao
                .findByRequirementTypeAndSecondValue(RequirementTypeEnum.BEEN_RACE, faction.getId().longValue())
                .stream().filter(current -> current.getObject().getDescription().equals(ObjectType.UNIT.name()))
                .map(current -> createUnitUpgradeRequiements(objectRelationBo.unboxObjectRelation(current),
                        current.getRequirements()))
                .collect(Collectors.toList());
    }

    /**
     * Finds a Bo by the requirement (useful for example to test for second, or
     * third value of the requirement)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public <K extends Serializable, E extends EntityWithId<K>, D extends DtoFromEntity<E>> BaseBo<K, E, D> findBoByRequirement(
            RequirementTypeEnum requirementType) {
        Class<?> clazz;
        switch (requirementType) {
            case UPGRADE_LEVEL:
                clazz = UpgradeBo.class;
                break;
            case HAVE_UNIT:
            case UNIT_AMOUNT:
                clazz = UnitBo.class;
                break;
            case BEEN_RACE:
                clazz = FactionBo.class;
                break;
            case HAVE_SPECIAL_LOCATION:
                clazz = SpecialLocationBo.class;
                break;
            case HAVE_SPECIAL_ENABLED:
            case HAVE_SPECIAL_AVAILABLE:
                clazz = TimeSpecialBo.class;
                break;
            case HOME_GALAXY:
                clazz = GalaxyBo.class;
                break;
            case WORST_PLAYER:
                throw new ProgrammingException("Requirement " + requirementType.name()
                        + "doesn't have a BO, you should check for it, prior to invoking this method");
            default:
                throw new SgtBackendNotImplementedException(
                        "Support for " + requirementType.name() + " has not been added yet");
        }
        return (BaseBo<K, E, D>) beanFactory.getBean(clazz);
    }

    /**
     * Checks requirements when race has been selected
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void triggerFactionSelection(UserStorage user) {
        processRelationList(requirementDao.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE),
                user);
    }

    /**
     * Chacks requirements when galaxy has been assigned
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void triggerHomeGalaxySelection(UserStorage user) {
        processRelationList(requirementDao.findObjectRelationsHavingRequirementType(RequirementTypeEnum.HOME_GALAXY),
                user);
    }

    /**
     * Checks requirements when level up mission has been completed!
     *
     * @param user
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void triggerLevelUpCompleted(UserStorage user, Integer upgradeId) {
        processRelationList(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.UPGRADE_LEVEL,
                upgradeId.longValue()), user);
    }

    /**
     * Checks requirements that has dependency on having this unit<br>
     * Following requirements are checked:
     * <ul>
     * <li>HAVE_UNIT</li>
     * <li>UNIT_AMOUNT</li>
     * </ul>
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void triggerUnitBuildCompletedOrKilled(UserStorage user, Unit unit) {
        processRelationList(requirementDao.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_UNIT,
                unit.getId().longValue()), user);
        triggerUnitAmountChanged(user, unit);
    }

    @Transactional
    public void triggerUnitAmountChanged(UserStorage user, Unit unit) {
        long count = obtainedUnitRepository.countByUserAndUnit(user, unit);
        processRelationList(requirementDao.findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(
                RequirementTypeEnum.UNIT_AMOUNT, unit.getId().longValue(), count), user);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void triggerSpecialLocation(UserStorage user, SpecialLocation specialLocation) {
        processRelationList(objectRelationBo.findByRequirementTypeAndSecondValue(
                RequirementTypeEnum.HAVE_SPECIAL_LOCATION, specialLocation.getId().longValue()), user);
    }

    @Transactional
    public void triggerTimeSpecialStateChange(UserStorage user, TimeSpecial timeSpecial) {
        processRelationList(
                objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, timeSpecial.getId().longValue()),
                user
        );
    }

    /**
     * Checks if all users met the new requirements of the changed relation
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void triggerRelationChanged(ObjectRelation relation) {
        ObjectRelation withSessionRelation = objectRelationBo.refresh(relation);
        List<UserStorage> users = userStorageBo.findAll();
        for (UserStorage user : users) {
            processRelation(withSessionRelation, user);
        }
    }

    /**
     * Checks if the input user has reached the level of the upgrades, and fills the
     * property <i>reached</i>, which is false by default
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public List<UnitWithRequirementInformation> computeReachedLevel(UserStorage user,
                                                                    List<UnitWithRequirementInformation> listToFill) {
        return listToFill.stream().map(currentUnit -> {
            currentUnit.getRequirements().forEach(currentRequirement -> {
                var obtainedUpgrade = obtainedUpgradeBo.findByUserAndUpgrade(user.getId(),
                        currentRequirement.getUpgrade().getId());
                currentRequirement.setReached(
                        obtainedUpgrade != null && obtainedUpgrade.getLevel() >= currentRequirement.getLevel());
            });
            return currentUnit;
        }).collect(Collectors.toList());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @Transactional
    public RequirementInformationDto addRequirementFromDto(RequirementInformationDto input) {
        ValidationUtil.getInstance().requireNotNull(input.getRequirement(), "requirement")
                .requireNull(input.getId(), "requirement.id").requireNotNull(input.getRelation(), "relation")
                .requireValidEnumValue(input.getRelation().getObjectCode(), ObjectEnum.class, "relation.objectCode")
                .requirePositiveNumber(input.getRelation().getReferenceId(), "relation.referenceId")
                .requireValidEnumValue(input.getRequirement().getCode(), RequirementTypeEnum.class, "requirement.code")
                .requireNotNull(input.getSecondValue(), "secondValue");
        RequirementInformation requirementInformation = new RequirementInformation();
        requirementInformation.setSecondValue(input.getSecondValue());
        requirementInformation.setThirdValue(input.getThirdValue());
        requirementInformation.setRelation(objectRelationBo.findObjectRelationOrCreate(
                ObjectEnum.valueOf(input.getRelation().getObjectCode()), input.getRelation().getReferenceId()));
        requirementInformation
                .setRequirement(findOneByCode(RequirementTypeEnum.valueOf(input.getRequirement().getCode())));
        requirementInformation = requirementInformationBo.save(requirementInformation);
        return dtoUtilService.dtoFromEntity(RequirementInformationDto.class, requirementInformation);
    }

    /**
     * Process the list of relations, and add or remove then from unlocked_relation
     * table if requirements are met or not
     *
     * @author Kevin Guanche Darias
     */
    private void processRelationList(List<ObjectRelation> relations, UserStorage user) {
        Set<ObjectRelation> affectedMasters = new HashSet<>();
        relations.forEach(currentRelation -> {
            var isSlaveOrHasNotSlaves = false;
            if (currentRelation.getObject().findCodeAsEnum() == ObjectEnum.REQUIREMENT_GROUP) {
                var relationWithMaster = objectRelationToObjectRelationBo
                        .findBySlave(currentRelation);
                if (relationWithMaster != null) {
                    affectedMasters.add(relationWithMaster.getMaster());
                } else {
                    LOG.warn("Orphan group with id " + currentRelation.getId());
                }
                isSlaveOrHasNotSlaves = true;
            } else {
                isSlaveOrHasNotSlaves = !objectRelationToObjectRelationBo.isMaster(currentRelation);
            }
            if (isSlaveOrHasNotSlaves) {
                processRelation(currentRelation, user);
            }
        });
        affectedMasters.forEach(master -> {
            List<ObjectRelation> slaves = objectRelationToObjectRelationBo.findByMasterId(master.getId()).stream()
                    .map(ObjectRelationToObjectRelation::getSlave).collect(Collectors.toList());
            if (slaves.stream().anyMatch(slave -> unlockedRelationBo.isUnlocked(user, slave))) {
                registerObtainedRelation(master, user);
            } else {
                unregisterLossedRelation(master, user);
            }
        });
    }

    /**
     * Process single relation change
     *
     * @param relation relation persisted entity
     * @author Kevin Guanche Darias
     */
    private void processRelation(ObjectRelation relation, UserStorage user) {
        if (checkRequirementsAreMet(relation, user)) {
            registerObtainedRelation(relation, user);
        } else {
            unregisterLossedRelation(relation, user);
        }
    }

    /**
     * Will check that all requirements are met for given relation and user
     *
     * @return True if object can be used
     * @author Kevin Guanche Darias
     */
    private boolean checkRequirementsAreMet(ObjectRelation objectRelation, UserStorage user) {
        for (RequirementInformation currentRequirement : objectRelation.getRequirements()) {
            boolean status;
            var requirementType = RequirementTypeEnum.valueOf(currentRequirement.getRequirement().getCode());
            switch (requirementType) {
                case UPGRADE_LEVEL:
                    status = checkUpgradeLevelRequirement(currentRequirement, user.getId());
                    break;
                case HAVE_UNIT:
                    status = checkHaveUnitRequirement(currentRequirement, user);
                    break;
                case UNIT_AMOUNT:
                    status = checkUnitAmountRequirement(currentRequirement, user);
                    break;
                case BEEN_RACE:
                    status = checkBeenFactionRequirement(currentRequirement, user.getId());
                    break;
                case HOME_GALAXY:
                    status = checkBeenGalaxyRequirement(currentRequirement, user);
                    break;
                case HAVE_SPECIAL_LOCATION:
                    status = checkSpecialLocationRequirement(currentRequirement, user);
                    break;
                default:
                    status = runRequirementSources(requirementType, currentRequirement, user);
                    break;
            }
            if (!status) {
                return false;
            }
        }
        return true;
    }

    private boolean runRequirementSources(RequirementTypeEnum requirementType, RequirementInformation requirementInformation, UserStorage user) {
        return requirementSources.stream()
                .filter(requirementSource -> requirementSource.supports(requirementType.name()))
                .findFirst()
                .map(requirementSource -> requirementSource.checkRequirementIsMet(requirementInformation, user))
                .orElseThrow(() -> new SgtBackendNotImplementedException("Not implemented requirement type: " + requirementInformation.getRequirement().getCode()));
    }

    private boolean checkUpgradeLevelRequirement(RequirementInformation requirementInformation, Integer userId) {
        var upgrade = upgradeBo.findById(requirementInformation.getSecondValue().intValue());
        int level;
        var obtainedUpgrade = obtainedUpgradeBo.findByUserAndUpgrade(userId, upgrade.getId());
        if (obtainedUpgrade == null) {
            level = 0;
        } else {
            level = obtainedUpgrade.getLevel();
        }
        return level >= requirementInformation.getThirdValue();
    }

    private boolean checkHaveUnitRequirement(RequirementInformation requirementInformation, UserStorage user) {
        return obtainedUnitRepository.isBuiltUnit(
                user,
                unitRepository.getOne(requirementInformation.getSecondValue().intValue())
        );
    }

    private boolean checkUnitAmountRequirement(RequirementInformation requirementInformation, UserStorage user) {
        return obtainedUnitRepository.countByUserAndUnit(user,
                unitRepository.getOne(requirementInformation.getSecondValue().intValue())) >= requirementInformation.getThirdValue();
    }

    private boolean checkSpecialLocationRequirement(RequirementInformation currentRequirement, UserStorage user) {
        var planet = planetBo.findOneBySpecialLocationId(currentRequirement.getSecondValue().intValue());
        if (planet == null) {
            LOG.warn("Special location " + currentRequirement.getSecondValue() + " is not assigned to any planet");
            return false;
        } else if (planet.getOwner() == null) {
            return false;
        } else {
            return planet.getOwner().getId().equals(user.getId());
        }
    }

    private boolean checkBeenFactionRequirement(RequirementInformation requirement, Integer userId) {
        return userStorageBo.isOfFaction(requirement.getSecondValue().intValue(), userId);
    }

    private boolean checkBeenGalaxyRequirement(RequirementInformation requirement, UserStorage user) {
        return user.getHomePlanet().getGalaxy().getId().equals(requirement.getSecondValue().intValue());
    }

    /**
     * Will register the obtained object <br />
     * <b>NOTICE: If relation already exists in unlocked_relation table will just do
     * nothing</b><br />
     * If relation is an upgrade, will save it to the ObtainedUpgrades!
     *
     * @author Kevin Guanche Darias
     */
    private void registerObtainedRelation(ObjectRelation relation, UserStorage user) {
        Integer userId = user.getId();
        if (findUnlockedObjectRelation(relation.getId(), userId) == null) {
            var unlockedRelation = new UnlockedRelation();
            unlockedRelation.setRelation(relation);
            unlockedRelation.setUser(user);
            unlockedRelationBo.save(unlockedRelation);
            var object = ObjectEnum.valueOf(relation.getObject().getCode());
            switch (object) {
                case UPGRADE:
                    if (obtainedUpgradeBo.userHasUpgrade(userId, relation.getReferenceId())) {
                        alterObtainedUpgradeAvailability(
                                obtainedUpgradeBo.findUserObtainedUpgrade(userId, relation.getReferenceId()), true);
                    } else {
                        registerObtainedUpgrade(user, relation.getReferenceId());
                    }
                    break;
                case UNIT:
                    emitUnlockedChange(unlockedRelation, object, unitBo);
                    break;
                case TIME_SPECIAL:
                    emitUnlockedChange(unlockedRelation, object, timeSpecialBo);
                    break;
                case REQUIREMENT_GROUP:
                    break;
                case SPEED_IMPACT_GROUP:
                    emitUnlockedSpeedImpactGroups(user);
                    break;
            }
        }
    }

    private void emitUnlockedSpeedImpactGroups(UserStorage user) {
        TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(user.getId(),
                "speed_impact_group_unlocked_change", () -> speedImpactGroupBo.findCrossGalaxyUnlocked(user)));
    }

    private void unregisterLossedRelation(ObjectRelation relation, UserStorage user) {
        UnlockedRelation unlockedRelation = findUnlockedObjectRelation(relation.getId(), user.getId());
        if (unlockedRelation != null) {
            unlockedRelationBo.delete(unlockedRelation.getId());
        }

        ObjectEnum object = ObjectEnum.valueOf(relation.getObject().getCode());
        if (object == ObjectEnum.UPGRADE && obtainedUpgradeBo.userHasUpgrade(user.getId(), relation.getReferenceId())) {
            alterObtainedUpgradeAvailability(
                    obtainedUpgradeBo.findUserObtainedUpgrade(user.getId(), relation.getReferenceId()), false);
        } else if (object == ObjectEnum.SPEED_IMPACT_GROUP) {
            emitUnlockedSpeedImpactGroups(user);
        } else if (unlockedRelation != null) {
            emitUnlockedChange(unlockedRelation, object, ObjectEnum.UNIT.equals(object) ? unitBo : timeSpecialBo);
        }
    }

    private UnlockedRelation findUnlockedObjectRelation(Integer relationId, Integer userId) {
        return unlockedRelationBo.findOneByUserIdAndRelationId(userId, relationId);
    }

    private void registerObtainedUpgrade(UserStorage user, Integer upgradeId) {
        ObtainedUpgrade obtainedUpgrade = new ObtainedUpgrade();
        obtainedUpgrade.setLevel(0);
        obtainedUpgrade.setUpgrade(upgradeBo.findById(upgradeId));
        obtainedUpgrade.setUserId(user);
        obtainedUpgrade.setAvailable(true);
        obtainedUpgradeBo.save(obtainedUpgrade);
    }

    private void alterObtainedUpgradeAvailability(ObtainedUpgrade obtainedUpgrade, Boolean available) {
        obtainedUpgrade.setAvailable(available);
        obtainedUpgradeBo.save(obtainedUpgrade);
    }

    private UnitWithRequirementInformation createUnitUpgradeRequiements(Unit unit,
                                                                        List<RequirementInformation> requirementInformations) {
        UnitWithRequirementInformation retVal = new UnitWithRequirementInformation();
        retVal.setUnit(new UnitDto());
        retVal.getUnit().dtoFromEntity(unit);
        retVal.setRequirements(requirementInformations.stream()
                .filter(current -> current.getRequirement().getCode().equals(RequirementTypeEnum.UPGRADE_LEVEL.name()))
                .map(current -> {
                    UnitUpgradeRequirements unitUpgradeRequirements = new UnitUpgradeRequirements();
                    unitUpgradeRequirements.setLevel(current.getThirdValue().intValue());
                    UpgradeDto upgradeDto = new UpgradeDto();
                    upgradeDto.dtoFromEntity(upgradeBo.findById(current.getSecondValue().intValue()));
                    unitUpgradeRequirements.setUpgrade(upgradeDto);
                    return unitUpgradeRequirements;
                }).collect(Collectors.toList()));
        return retVal;
    }

    private void emitUnlockedChange(UnlockedRelation unlockedRelation, ObjectEnum object, WithUnlockableBo bo) {
        Integer userId = unlockedRelation.getUser().getId();
        String eventPrefix = object.name().toLowerCase();
        TransactionUtil.doAfterCommit(() -> {
            if (entityManager.contains(unlockedRelation)) {
                entityManager.refresh(unlockedRelation);
            }
            socketIoService.sendMessage(userId, eventPrefix + "_unlocked_change",
                    () -> dtoUtilService.convertEntireArray(bo.getDtoClass(), bo.findUnlocked(userId)));
        });
    }
}
