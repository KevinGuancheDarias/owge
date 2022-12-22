package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

import static com.kevinguanchedarias.owgejava.entity.RequirementGroup.REQUIREMENT_GROUP_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.entity.RequirementInformation.REQUIREMENT_INFORMATION_CACHE_TAG;

@Component
@Transactional
public class RequirementInformationBo implements BaseBo<Integer, RequirementInformation, RequirementInformationDto> {
    @Serial
    private static final long serialVersionUID = 4755638529538733332L;

    @Autowired
    private RequirementInformationDao requirementInformationDao;

    @Autowired
    private RequirementBo requirementBo;

    @Autowired
    private RequirementInformationRepository repository;

    @Autowired
    private transient TaggableCacheManager taggableCacheManager;

    @Override
    public JpaRepository<RequirementInformation, Integer> getRepository() {
        return repository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<RequirementInformationDto> getDtoClass() {
        return RequirementInformationDto.class;
    }

    /**
     * Saves to database and <b>triggers relation changed!</b>
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public RequirementInformation save(RequirementInformation requirementInformation) {
        checkSecondValue(requirementInformation);
        RequirementInformation savedRequirement = requirementInformationDao.save(requirementInformation);
        ObjectRelation relation = requirementInformation.getRelation();
        requirementBo.triggerRelationChanged(relation);
        return savedRequirement;
    }

    /**
     * Checks if the specified second value id exists
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public void checkSecondValue(RequirementInformation requirementInformation) {
        RequirementTypeEnum requirement = RequirementTypeEnum
                .valueOf(requirementInformation.getRequirement().getCode());
        if (requirement != RequirementTypeEnum.WORST_PLAYER) {
            requirementBo.findBoByRequirement(requirement)
                    .existsOrDie(requirementInformation.getSecondValue().intValue());
        }
    }

    @Transactional
    public void delete(Integer id) {
        delete(findByIdOrDie(id));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.kevinguanchedarias.owgejava.business.BaseBo#delete(com.kevinguanchedarias
     * .kevinsuite.commons.entity.EntityWithId)
     */
    @Transactional
    public void delete(RequirementInformation entity) {
        entity.getRelation().getRequirements().removeIf(current -> current.getId().equals(entity.getId()));
        repository.delete(entity);
        var relation = entity.getRelation();
        requirementBo.triggerRelationChanged(relation);
        taggableCacheManager.evictByCacheTag(
                REQUIREMENT_INFORMATION_CACHE_TAG,
                "#" + relation.getObject().getCode() + "_" + relation.getReferenceId()
        );
        taggableCacheManager.evictByCacheTag(REQUIREMENT_GROUP_CACHE_TAG);
    }

    /**
     * Will return requirement for specified object type with the given referenceId
     *
     * @param objectEnum  Type of object
     * @param referenceId Id on the target entity, for example id of an upgrade, or
     *                    an unit
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public List<RequirementInformation> findRequirements(ObjectEnum objectEnum, Integer referenceId) {
        return requirementInformationDao.findRequirements(objectEnum, referenceId);
    }
}
