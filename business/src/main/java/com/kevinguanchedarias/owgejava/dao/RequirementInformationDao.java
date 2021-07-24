package com.kevinguanchedarias.owgejava.dao;

import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.SpecialLocationBo;
import com.kevinguanchedarias.owgejava.business.UpgradeBo;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithRequirementGroupsListener;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.ObjectType;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendRequirementException;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RequirementInformationDao implements Serializable {
    private static final long serialVersionUID = -4922698439719271164L;
    private static final Logger LOG = Logger.getLogger(RequirementInformationDao.class);
    private static final String CACHE_KEY = "requirements";

    @Autowired
    private RequirementInformationRepository requirementInformationRepository;

    @Autowired
    private SpecialLocationBo specialLocationBo;

    @Autowired
    private FactionBo factionBo;

    @Autowired
    private UpgradeBo upgradeBo;

    @Autowired
    private GalaxyBo galaxyBo;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private ObjectRelationBo objectRelationsBo;

    @Autowired
    private ObjectRelationsRepository objectRelationsRepository;

    @Autowired
    private transient ExceptionUtilService exceptionUtilService;

    @Autowired
    private transient CacheManager cacheManager;

    public List<RequirementInformation> findAll() {
        return requirementInformationRepository.findAll();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use {@link ObjectRelationBo#findAll()}
     */
    @Deprecated(since = "0.8.0")
    public List<ObjectRelation> findAllObjectRelations() {
        return objectRelationsRepository.findAll();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use
     * {@link ObjectRelationBo#findObjectRelationsHavingRequirementType(RequirementTypeEnum)}
     */
    @Deprecated(since = "0.8.0")
    public List<ObjectRelation> findObjectRelationsHavingRequirementType(RequirementTypeEnum type) {
        return objectRelationsRepository.findByRequirementsRequirementCode(type.name());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use
     * {@link ObjectRelationBo#findObjectRelationsOfTypeHavingRequirementType(ObjectType, RequirementTypeEnum)}
     */
    @Deprecated(since = "0.8.0")
    public List<ObjectRelation> findObjectRelationsOfTypeHavingRequirementType(ObjectType type,
                                                                               RequirementTypeEnum requirementType) {
        return objectRelationsRepository.findByObjectDescriptionAndRequirementsRequirementCode(type.name(),
                requirementType.name());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @deprecated Use
     * {@link ObjectRelationBo#findByRequirementTypeAndSecondValue(RequirementTypeEnum, Long)}
     */
    @Deprecated(since = "0.8.0")
    public List<ObjectRelation> findByRequirementTypeAndSecondValue(RequirementTypeEnum type, Long secondValue) {
        return objectRelationsRepository.findByRequirementsRequirementCodeAndRequirementsSecondValue(type.name(),
                secondValue);
    }

    /**
     * Finds by type, secondValue, and where thirdValue is greater or equal to x<br>
     * Example resultant SQL: WHERE type = '$type' AND secondValue = '$secondValue'
     * AND thirdValue >= '$thidValue'
     *
     * @author Kevin Guanche Darias
     * @deprecated Use
     * {@link ObjectRelationBo#findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(RequirementTypeEnum, Long, Long)}
     */
    @Deprecated(since = "0.8.0")
    public List<ObjectRelation> findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(
            RequirementTypeEnum type, Long secondValue, Long thirdValue) {
        return objectRelationsRepository
                .findByRequirementsRequirementCodeAndRequirementsSecondValueAndRequirementsThirdValueGreaterThanEqual(
                        type.name(), secondValue, thirdValue);
    }

    /**
     * Will return requirement for specified object type
     *
     * @param targetObject - Type of object
     * @param referenceId  - Id on the target entity, for example id of an upgrade,
     *                     or an unit
     * @author Kevin Guanche Darias
     * @deprecated Use
     * {@link RequirementInformationDao#findRequirements(ObjectEnum, Integer)}
     */
    @Deprecated(since = "0.8.0")
    public List<RequirementInformation> getRequirements(RequirementTargetObject targetObject, Integer referenceId) {
        ObjectRelation objectRelation = getObjectRelation(targetObject, referenceId);
        return returnRequirementOrEmptyList(objectRelation);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @Cacheable(cacheNames = CACHE_KEY, key = "{ #objectEnum, #referenceId }")
    public List<RequirementInformation> findRequirements(ObjectEnum objectEnum, Integer referenceId) {
        ObjectRelation objectRelation = objectRelationsBo.findOne(objectEnum, referenceId);
        return objectRelation != null ? requirementInformationRepository.findByRelationId(objectRelation.getId())
                : new ArrayList<>();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use
     * {@link RequirementInformationDao#findRequirementsByType(ObjectEnum, Integer, RequirementTypeEnum)}
     */
    @Deprecated(since = "0.8.0")
    public List<RequirementInformation> findRequirementsByType(RequirementTargetObject targetObject,
                                                               Integer referenceId, RequirementTypeEnum type) {
        ObjectRelation objectRelation = getObjectRelation(targetObject, referenceId);
        return findRequirementsByType(objectRelation, type);
    }

    /**
     * Find all the requirements of the same type that a reference has
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<RequirementInformation> findRequirementsByType(ObjectEnum target, Integer referenceId,
                                                               RequirementTypeEnum type) {
        ObjectRelation objectRelation = objectRelationsBo.findOne(target, referenceId);
        return findRequirementsByType(objectRelation, type);
    }

    public List<RequirementInformation> findRequirementsByType(ObjectRelation objectRelation,
                                                               RequirementTypeEnum type) {
        if (objectRelation == null) {
            return new ArrayList<>();
        } else {
            return requirementInformationRepository.findByRelationIdAndRequirementId(objectRelation.getId(),
                    type.getValue());
        }
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use {@link ObjectRelationBo#findOne(ObjectEnum, Integer)}
     */
    @Deprecated(since = "0.8.0")
    public ObjectRelation getObjectRelation(RequirementTargetObject targetObject, Integer referenceId) {
        return objectRelationsRepository.findOneByObjectDescriptionAndReferenceId(targetObject.name(), referenceId);
    }

    /**
     * Will save the requirement information to the database
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public RequirementInformation save(RequirementInformation requirementInformation) {
        objectRelationsBo.checkValid(requirementInformation.getRelation());
        try {
            requirementInformation = requirementInformationRepository.save(requirementInformation);

        } catch (Exception e) {
            if (ExceptionUtilService.isSqlDuplicatedKey(e)) {
                LOG.debug("Duplicated entry", e);
                throw exceptionUtilService
                        .createExceptionBuilder(SgtBackendInvalidInputException.class,
                                "I18N_ERR_DUPLICATED_REQUIREMENT")
                        .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
            } else {
                throw e;
            }
        }
        List<RequirementInformation> storedRequirementsInformation = requirementInformation.getRelation()
                .getRequirements();
        if (storedRequirementsInformation == null) {
            storedRequirementsInformation = new ArrayList<>();
        }
        storedRequirementsInformation.add(requirementInformation);
        requirementInformation.getRelation().setRequirements(storedRequirementsInformation);
        requirementInformation.setRelation(objectRelationsBo.save(requirementInformation.getRelation()));
        clearCache();
        return requirementInformation;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @deprecated Use
     * {@link RequirementInformationDao#deleteRequirementInformationByObjectRelation(ObjectEnum, Integer)}
     * which <b>doesn't delete the ObjectRelation by itself
     */
    @Transactional
    @Deprecated(since = "0.8.0")
    public void deleteAllObjectRelations(RequirementTargetObject target, Integer referenceId) {
        var objectRelation = getObjectRelation(target, referenceId);
        objectRelationsRepository.deleteById(objectRelation.getId());
        if (target == RequirementTargetObject.UPGRADE) {
            Integer upgradeLevelRequirementId = requirementRepository
                    .findOneByCode(RequirementTypeEnum.UPGRADE_LEVEL.name()).getId();
            requirementInformationRepository.deleteByRequirementIdAndSecondValue(upgradeLevelRequirementId,
                    referenceId.longValue());
        }
    }

    /**
     * Deletes all requirement information for given ObjectRelation (if exists)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteRequirementInformationByObjectRelation(ObjectEnum target, Integer referenceId) {
        var objectRelation = objectRelationsBo.findOne(target, referenceId);
        if (objectRelation != null && target.equals(ObjectEnum.UPGRADE)) {
            Integer upgradeLevelRequirementId = requirementRepository
                    .findOneByCode(RequirementTypeEnum.UPGRADE_LEVEL.name()).getId();
            requirementInformationRepository.deleteByRequirementIdAndSecondValue(upgradeLevelRequirementId,
                    referenceId.longValue());
        }
    }

    @Transactional(readOnly = false)
    public void deleteRequirementInformation(RequirementInformation requirementInformation) {
        requirementInformationRepository.deleteById(requirementInformation.getId());
        requirementInformationRepository.flush();
        objectRelationsRepository.flush();
    }

    /**
     * Gets the human friendly second value description<br />
     * For example: "{upgrade_name} level some"
     *
     * @author Kevin Guanche Darias
     */
    public String getSecondValueDescription(RequirementInformation requirementInformation) {
        String retVal;
        switch (requirementInformation.getRequirement().getCode()) {
            case "HAVE_SPECIAL_LOCATION":
                retVal = specialLocationBo.findById(requirementInformation.getSecondValue().intValue()).getName();
                break;
            case "BEEN_RACE":
                retVal = factionBo.findById(requirementInformation.getSecondValue().intValue()).getName();
                break;
            case "UPGRADE_LEVEL":
                retVal = upgradeBo.findById(requirementInformation.getSecondValue().intValue()).getName() + " nivel "
                        + requirementInformation.getThirdValue();
                break;
            case "WORST_PLAYER":
                retVal = "El tío más noob!";
                break;
            case "HOME_GALAXY":
                retVal = galaxyBo.findById(requirementInformation.getSecondValue().intValue()).getName();
                break;
            default:
                throw new SgtBackendRequirementException("No existe este tipo de requisito");
        }
        return retVal;
    }

    /**
     * Will find all requirement information for given object type and requirement
     * id
     *
     * @author Kevin Guanche Darias
     */
    @Deprecated(since = "0.8.0")
    public List<RequirementInformation> findByObjectTypeAndRequirementId(RequirementTargetObject objectType,
                                                                         RequirementTypeEnum requirement) {
        return requirementInformationRepository.findByRelationObjectDescriptionAndRequirementId(objectType.name(),
                requirement.getValue());
    }

    public void clearCache() {
        doClearCache(CACHE_KEY);
        doClearCache(EntityWithRequirementGroupsListener.CACHE_KEY);
    }

    private void doClearCache(String cacheKey) {
        var cache = this.cacheManager.getCache(cacheKey);
        if (cache != null) {
            cache.clear();
        } else {
            LOG.warn("Cache object for key " + cacheKey + " is null, da' fuck??");
        }
    }

    /**
     * Returns the requirement by object relation, but if null, returns empty list
     *
     * @author Kevin Guanche Darias
     */
    private List<RequirementInformation> returnRequirementOrEmptyList(ObjectRelation objectRelation) {
        if (objectRelation == null) {
            return new ArrayList<>();
        } else {

            return objectRelation.getRequirements();
        }
    }
}
