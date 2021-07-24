package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class RequirementInformationBo implements BaseBo<Integer, RequirementInformation, RequirementInformationDto> {
    private static final long serialVersionUID = 4755638529538733332L;

    @Autowired
    @Lazy
    private RequirementInformationDao requirementInformationDao;

    @Autowired
    private RequirementBo requirementBo;

    @Autowired
    private RequirementInformationRepository repository;

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getRepository()
     */
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
     * Deletes a requirement information from database<br />
     * <b>NOTICE: Triggers a unlocked relations update</b>
     *
     * @param requirementInformation
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void deleteRequirementInformation(RequirementInformation requirementInformation) {
        ObjectRelation affectedRelation = requirementInformation.getRelation();
        requirementInformationDao.deleteRequirementInformation(requirementInformation);
        requirementBo.triggerRelationChanged(affectedRelation);
    }

    /**
     * Deletes all requirement informations <br>
     * <b>IMPORTANT:</b> Doesn't trigger relation changed, as we are probably going
     * to delete the relation
     *
     * @param relation
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional
    public void deleteByRelation(ObjectRelation relation) {
        repository.deleteByRelation(relation);
    }

    public String getSecondValueDescription(RequirementInformation requirementInformation) {
        return requirementInformationDao.getSecondValueDescription(requirementInformation);
    }

    /**
     * @param targetObject
     * @param referenceId
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated Use {@link ObjectRelationBo} instead
     */
    @Deprecated(since = "0.8.0")
    public ObjectRelation findObjectRelation(RequirementTargetObject targetObject, Integer referenceId) {
        return requirementInformationDao.getObjectRelation(targetObject, referenceId);
    }

    /**
     * Saves to database and <b>triggers relation changed!</b>
     *
     * @param requirementInformation
     * @author Kevin Guanche Darias
     */
    @Override
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
     * @param requirementInformation
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

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#delete(java.lang.Number)
     */
    @Transactional
    @Override
    public void delete(Integer id) {
        delete(findByIdOrDie(id));
        requirementInformationDao.clearCache();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.kevinguanchedarias.owgejava.business.BaseBo#delete(com.kevinguanchedarias
     * .kevinsuite.commons.entity.EntityWithId)
     */
    @Transactional
    @Override
    public void delete(RequirementInformation entity) {
        entity.getRelation().getRequirements().removeIf(current -> current.getId().equals(entity.getId()));
        BaseBo.super.delete(entity);
        requirementBo.triggerRelationChanged(entity.getRelation());
        requirementInformationDao.clearCache();
    }
}
