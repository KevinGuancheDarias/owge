package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.requirement.RequirementSource;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnlockedSpeedImpactGroupService;
import com.kevinguanchedarias.owgejava.business.timespecial.UnlockableTimeSpecialService;
import com.kevinguanchedarias.owgejava.business.unit.UnlockableUnitService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.dto.UpgradeDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.ObjectType;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.InvalidConfigurationException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.pojo.UnitUpgradeRequirements;
import com.kevinguanchedarias.owgejava.pojo.UnitWithRequirementInformation;
import com.kevinguanchedarias.owgejava.repository.*;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.ValidationUtil;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheEvictByTag;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.kevinguanchedarias.owgejava.entity.Faction.FACTION_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.entity.RequirementInformation.REQUIREMENT_INFORMATION_CACHE_TAG;

@Component
@Transactional
@AllArgsConstructor
public class RequirementBo implements Serializable {
    @Serial
    private static final long serialVersionUID = -7069590234333605969L;

    private static final Logger LOG = Logger.getLogger(RequirementBo.class);
    private final RequirementRepository requirementRepository;
    private final UnlockedRelationBo unlockedRelationBo;
    private final UpgradeBo upgradeBo;
    private final ObjectRelationBo objectRelationBo;
    private final ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo;
    private final DtoUtilService dtoUtilService;
    private final RequirementInformationRepository requirementInformationRepository;
    private final transient AutowireCapableBeanFactory beanFactory;
    private final transient SocketIoService socketIoService;

    private final transient UnlockableTimeSpecialService unlockableTimeSpecialService;
    private final transient UnlockableUnitService unlockableUnitService;
    private final transient UnlockedSpeedImpactGroupService unlockedSpeedImpactGroupService;
    private final PlanetRepository planetRepository;
    private final transient EntityManager entityManager;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final UnitRepository unitRepository;
    private final transient List<RequirementSource> requirementSources;
    private final transient TransactionUtilService transactionUtilService;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;
    private final UnlockedRelationRepository unlockedRelationRepository;
    private final UserStorageRepository userStorageRepository;

    /**
     * Checks that the {@link RequirementTypeEnum} enum matches the database values
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @PostConstruct
    public void init() {
        Map<String, Requirement> requirementsMap = new HashMap<>();
        var validValues = Arrays.stream(RequirementTypeEnum.values()).filter(
                enumEntry -> enumEntry.getValue() > 0
        ).toList();
        try {
            findAll().forEach(current -> requirementsMap.put(current.getCode(), current));
            if (requirementsMap.size() != validValues.size()) {
                throw new SgtCorruptDatabaseException("Database Stored values don't match  enum values");
            }
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

    @TaggableCacheable(tags = {
            FACTION_CACHE_TAG + ":#faction.id",
            REQUIREMENT_INFORMATION_CACHE_TAG
    }, keySuffix = "#faction.id")
    public List<UnitWithRequirementInformation> findFactionUnitLevelRequirements(Faction faction) {
        return objectRelationBo
                .findByRequirementTypeAndSecondValue(RequirementTypeEnum.BEEN_RACE, faction.getId().longValue())
                .stream().filter(current -> current.getObject().getCode().equals(ObjectType.UNIT.name()))
                .map(current -> createUnitUpgradeRequirements(objectRelationBo.unboxObjectRelation(current),
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
        Class<?> clazz = switch (requirementType) {
            case UPGRADE_LEVEL, UPGRADE_LEVEL_LOWER_THAN -> UpgradeBo.class;
            case HAVE_UNIT, UNIT_AMOUNT -> UnitBo.class;
            case BEEN_RACE -> FactionBo.class;
            case HAVE_SPECIAL_LOCATION -> SpecialLocationBo.class;
            case HAVE_SPECIAL_ENABLED, HAVE_SPECIAL_AVAILABLE -> TimeSpecialBo.class;
            case HOME_GALAXY -> GalaxyBo.class;
            default -> throw new SgtBackendNotImplementedException(
                    "Support for " + requirementType.name() + " has not been added yet");
        };
        return (BaseBo<K, E, D>) beanFactory.getBean(clazz);
    }

    /**
     * Checks requirements when race has been selected
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void triggerFactionSelection(UserStorage user) {
        processRelationList(objectRelationBo.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE),
                user);
    }

    /**
     * Chacks requirements when galaxy has been assigned
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void triggerHomeGalaxySelection(UserStorage user) {
        processRelationList(objectRelationBo.findObjectRelationsHavingRequirementType(RequirementTypeEnum.HOME_GALAXY),
                user);
    }

    /**
     * Checks requirements when level up mission has been completed!
     *
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
        processRelationList(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_UNIT,
                unit.getId().longValue()), user);
        triggerUnitAmountChanged(user, unit);
    }

    @Transactional
    public void triggerUnitAmountChanged(UserStorage user, Unit unit) {
        long count = obtainedUnitRepository.countByUserAndUnit(user, unit);
        processRelationList(objectRelationBo.findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(
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
        var users = userStorageRepository.findAll();
        for (UserStorage user : users) {
            processRelation(withSessionRelation, user);
        }
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @Transactional
    @TaggableCacheEvictByTag(tags = REQUIREMENT_INFORMATION_CACHE_TAG + "#input.relation.objectCode_#input.relation.referenceId")
    public RequirementInformationDto addRequirementFromDto(RequirementInformationDto input) {
        ValidationUtil.getInstance().requireNotNull(input.getRequirement(), "requirement")
                .requireNull(input.getId(), "requirement.id").
                requireNotNull(input.getRelation(), "relation")
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
        requirementInformation = requirementInformationRepository.save(requirementInformation);
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
                    .map(ObjectRelationToObjectRelation::getSlave).toList();
            if (slaves.stream().anyMatch(slave -> unlockedRelationBo.isUnlocked(user, slave))) {
                registerObtainedRelation(master, user);
            } else {
                unregisterLostRelation(master, user);
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
            unregisterLostRelation(relation, user);
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
            status = switch (requirementType) {
                case UPGRADE_LEVEL -> checkUpgradeLevelRequirement(currentRequirement, user.getId());
                case HAVE_UNIT -> checkHaveUnitRequirement(currentRequirement, user);
                case UNIT_AMOUNT -> checkUnitAmountRequirement(currentRequirement, user);
                case BEEN_RACE -> checkBeenFactionRequirement(currentRequirement, user.getId());
                case HOME_GALAXY -> checkBeenGalaxyRequirement(currentRequirement, user);
                case HAVE_SPECIAL_LOCATION -> checkSpecialLocationRequirement(currentRequirement, user);
                default -> runRequirementSources(requirementType, currentRequirement, user);
            };
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
        var obtainedUpgrade = obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(userId, upgrade.getId());
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
                unitRepository.getReferenceById(requirementInformation.getSecondValue().intValue())
        );
    }

    private boolean checkUnitAmountRequirement(RequirementInformation requirementInformation, UserStorage user) {
        return obtainedUnitRepository.countByUserAndUnit(user,
                unitRepository.getReferenceById(requirementInformation.getSecondValue().intValue())) >= requirementInformation.getThirdValue();
    }

    private boolean checkSpecialLocationRequirement(RequirementInformation currentRequirement, UserStorage user) {
        var planet = planetRepository.findOneBySpecialLocationId(currentRequirement.getSecondValue().intValue());
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
        return userStorageRepository.isOfFaction(requirement.getSecondValue().intValue(), userId) != null;
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
        if (unlockedRelationBo.findOneByUserIdAndRelationId(userId, relation.getId()) == null) {
            var unlockedRelation = new UnlockedRelation();
            unlockedRelation.setRelation(relation);
            unlockedRelation.setUser(user);
            unlockedRelationRepository.save(unlockedRelation);
            var object = ObjectEnum.valueOf(relation.getObject().getCode());
            switch (object) {
                case UPGRADE:
                    if (obtainedUpgradeRepository.existsByUserIdAndUpgradeId(userId, relation.getReferenceId())) {
                        alterObtainedUpgradeAvailability(
                                obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(userId, relation.getReferenceId()), true);
                    } else {
                        registerObtainedUpgrade(user, relation.getReferenceId());
                    }
                    break;
                case UNIT:
                    emitUnlockedChange(unlockedRelation, object, unlockableUnitService);
                    break;
                case TIME_SPECIAL:
                    emitUnlockedChange(unlockedRelation, object, unlockableTimeSpecialService);
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
        transactionUtilService.doAfterCommit(() -> socketIoService.sendMessage(user.getId(),
                "speed_impact_group_unlocked_change", () -> unlockedSpeedImpactGroupService.findCrossGalaxyUnlocked(user)));
    }

    private void unregisterLostRelation(ObjectRelation relation, UserStorage user) {
        UnlockedRelation unlockedRelation = unlockedRelationBo.findOneByUserIdAndRelationId(user.getId(), relation.getId());
        if (unlockedRelation != null) {
            unlockedRelationRepository.delete(unlockedRelation);
        }

        ObjectEnum object = ObjectEnum.valueOf(relation.getObject().getCode());
        if (object == ObjectEnum.UPGRADE && obtainedUpgradeRepository.existsByUserIdAndUpgradeId(user.getId(), relation.getReferenceId())) {
            alterObtainedUpgradeAvailability(
                    obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(user.getId(), relation.getReferenceId()), false);
        } else if (object == ObjectEnum.SPEED_IMPACT_GROUP) {
            emitUnlockedSpeedImpactGroups(user);
        } else if (unlockedRelation != null) {
            emitUnlockedChange(unlockedRelation, object, ObjectEnum.UNIT.equals(object) ? unlockableUnitService : unlockableTimeSpecialService);
        }
    }

    private void registerObtainedUpgrade(UserStorage user, Integer upgradeId) {
        ObtainedUpgrade obtainedUpgrade = new ObtainedUpgrade();
        obtainedUpgrade.setLevel(0);
        obtainedUpgrade.setUpgrade(upgradeBo.findById(upgradeId));
        obtainedUpgrade.setUser(user);
        obtainedUpgrade.setAvailable(true);
        obtainedUpgradeRepository.save(obtainedUpgrade);
    }

    private void alterObtainedUpgradeAvailability(ObtainedUpgrade obtainedUpgrade, Boolean available) {
        obtainedUpgrade.setAvailable(available);
        obtainedUpgradeRepository.save(obtainedUpgrade);
    }

    private UnitWithRequirementInformation createUnitUpgradeRequirements(Unit unit,
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void emitUnlockedChange(UnlockedRelation unlockedRelation, ObjectEnum object, WithUnlockableBo bo) {
        Integer userId = unlockedRelation.getUser().getId();
        String eventPrefix = object.name().toLowerCase();
        transactionUtilService.doAfterCommit(() -> {
            if (entityManager.contains(unlockedRelation)) {
                entityManager.refresh(unlockedRelation);
            }
            socketIoService.sendMessage(userId, eventPrefix + "_unlocked_change",
                    () -> dtoUtilService.convertEntireArray(bo.getDtoClass(), bo.findUnlocked(userId)));
        });
    }
}
