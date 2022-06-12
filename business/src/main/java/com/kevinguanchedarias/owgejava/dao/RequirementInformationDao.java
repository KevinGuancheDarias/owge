package com.kevinguanchedarias.owgejava.dao;

import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.SpecialLocationBo;
import com.kevinguanchedarias.owgejava.business.UpgradeBo;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
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
     * @since 0.8.0
     */
    @Cacheable(cacheNames = CACHE_KEY, key = "{ #objectEnum, #referenceId }")
    public List<RequirementInformation> findRequirements(ObjectEnum objectEnum, Integer referenceId) {
        ObjectRelation objectRelation = objectRelationsBo.findOne(objectEnum, referenceId);
        return objectRelation != null ? requirementInformationRepository.findByRelationId(objectRelation.getId())
                : new ArrayList<>();
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
        return requirementInformation;
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

    public void clearCache() {
        var cache = this.cacheManager.getCache(CACHE_KEY);
        if (cache != null) {
            cache.clear();
        } else {
            LOG.warn("Cache object is null, da' fuck??");
        }
    }
}
