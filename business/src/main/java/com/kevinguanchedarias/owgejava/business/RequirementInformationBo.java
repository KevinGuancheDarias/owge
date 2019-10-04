package com.kevinguanchedarias.owgejava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;

@Component
@Transactional
public class RequirementInformationBo implements BaseBo<RequirementInformation> {
	private static final long serialVersionUID = 4755638529538733332L;

	@Autowired
	private RequirementInformationDao requirementInformationDao;

	@Autowired
	private RequirementBo requirementBo;

	@Autowired
	private RequirementInformationRepository repository;

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

	public String getSecondValueDescription(RequirementInformation requirementInformation) {
		return requirementInformationDao.getSecondValueDescription(requirementInformation);
	}

	/**
	 * 
	 * @deprecated Use {@link ObjectRelationBo} instead
	 * 
	 * @param targetObject
	 * @param referenceId
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
	public void delete(Number id) {
		delete(findByIdOrDie(id));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.business.BaseBo#delete(com.kevinguanchedarias
	 * .kevinsuite.commons.entity.SimpleIdEntity)
	 */
	@Transactional
	@Override
	public void delete(RequirementInformation entity) {
		entity.getRelation().getRequirements().removeIf(current -> current.getId().equals(entity.getId()));
		BaseBo.super.delete(entity);
		requirementBo.triggerRelationChanged(entity.getRelation());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getRepository()
	 */
	@Override
	public JpaRepository<RequirementInformation, Number> getRepository() {
		return repository;
	}
}
