package com.kevinguanchedarias.sgtjava.business;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.dao.RequirementInformationDao;
import com.kevinguanchedarias.sgtjava.entity.ObjectRelation;
import com.kevinguanchedarias.sgtjava.entity.RequirementInformation;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;

@Component
@Transactional
public class RequirementInformationBo implements Serializable {
	private static final long serialVersionUID = 4755638529538733332L;

	@Autowired
	private RequirementInformationDao requirementInformationDao;

	@Autowired
	private RequirementBo requirementBo;

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

	public ObjectRelation findObjectRelation(RequirementTargetObject targetObject, Integer referenceId) {
		return requirementInformationDao.getObjectRelation(targetObject, referenceId);
	}

	/**
	 * Saves to database and <b>triggers relation changed!</b>
	 * 
	 * @param requirementInformation
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void save(RequirementInformation requirementInformation) {
		requirementInformationDao.save(requirementInformation);
		ObjectRelation relation = requirementInformation.getRelation();
		requirementBo.triggerRelationChanged(relation);
	}
}
