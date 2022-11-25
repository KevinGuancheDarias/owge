package com.kevinguanchedarias.owgejava.dao;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheEvictByTag;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.kevinguanchedarias.owgejava.business.RequirementGroupBo.REQUIREMENT_GROUP_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.RequirementInformationBo.REQUIREMENT_INFORMATION_CACHE_TAG;

@Repository
public class RequirementInformationDao implements Serializable {
    @Serial
    private static final long serialVersionUID = -4922698439719271164L;

    private static final Logger LOG = Logger.getLogger(RequirementInformationDao.class);

    @Autowired
    private RequirementInformationRepository requirementInformationRepository;
    
    @Autowired
    private ObjectRelationBo objectRelationsBo;

    @Autowired
    private ObjectRelationsRepository objectRelationsRepository;

    @Autowired
    private transient ExceptionUtilService exceptionUtilService;

    @Autowired
    private transient TaggableCacheManager taggableCacheManager;

    public List<RequirementInformation> findAll() {
        return requirementInformationRepository.findAll();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @TaggableCacheable(tags = {
            REQUIREMENT_GROUP_CACHE_TAG,
            REQUIREMENT_INFORMATION_CACHE_TAG + ":#objectEnum_#referenceId"
    })
    public List<RequirementInformation> findRequirements(ObjectEnum objectEnum, Integer referenceId) {
        ObjectRelation objectRelation = objectRelationsBo.findOne(objectEnum, referenceId);
        return objectRelation != null ? requirementInformationRepository.findByRelationId(objectRelation.getId())
                : new ArrayList<>();
    }

    /**
     * Will save the requirement information to the database
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    @TaggableCacheEvictByTag(tags = {
            REQUIREMENT_GROUP_CACHE_TAG,
            REQUIREMENT_INFORMATION_CACHE_TAG + ":#requirementInformation.relation.object.code_#requirementInformation.relation.referenceId"
    })
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
        requirementInformation.setRelation(objectRelationsRepository.save(requirementInformation.getRelation()));
        return requirementInformation;
    }

    public void clearCache() {
        taggableCacheManager.evictByCacheTag(REQUIREMENT_INFORMATION_CACHE_TAG);
    }
}
