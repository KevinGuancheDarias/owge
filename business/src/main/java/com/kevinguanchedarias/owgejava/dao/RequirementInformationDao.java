package com.kevinguanchedarias.owgejava.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.business.ObjectEntityBo;
import com.kevinguanchedarias.owgejava.business.SpecialLocationBo;
import com.kevinguanchedarias.owgejava.business.UpgradeBo;
import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.ObjectType;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendRequirementException;
import com.kevinguanchedarias.owgejava.repository.ObjectEntityRepository;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;
import com.kevinguanchedarias.owgejava.repository.WithNameRepository;

@Repository
public class RequirementInformationDao implements Serializable {
	private static final long serialVersionUID = -4922698439719271164L;

	@Autowired
	private ObjectRelationsRepository objectRelationsRepository;

	@Autowired
	private ObjectEntityRepository objectEntityRepository;

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
	private ObjectEntityBo objectEntityBo;

	public List<RequirementInformation> findAll() {
		return requirementInformationRepository.findAll();
	}

	public List<ObjectRelation> findAllObjectRelations() {
		return objectRelationsRepository.findAll();
	}

	public List<ObjectRelation> findObjectRelationsHavingRequirementType(RequirementType type) {
		return objectRelationsRepository.findByRequirementsRequirementCode(type.name());
	}

	public List<ObjectRelation> findObjectRelationsOfTypeHavingRequirementType(ObjectType type,
			RequirementType requirementType) {
		return objectRelationsRepository.findByObjectDescriptionAndRequirementsRequirementCode(type.name(),
				requirementType.name());
	}

	public List<ObjectRelation> findByRequirementTypeAndSecondValue(RequirementType type, Long secondValue) {
		return objectRelationsRepository.findByRequirementsRequirementCodeAndRequirementsSecondValue(type.name(),
				secondValue);
	}

	/**
	 * Finds by type, secondValue, and where thirdValue is greater or equal to
	 * x<br>
	 * Example resultant SQL: WHERE type = '$type' AND secondValue =
	 * '$secondValue' AND thirdValue >= '$thidValue'
	 * 
	 * @param type
	 * @param secondValue
	 * @param thirdValue
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<ObjectRelation> findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(RequirementType type,
			Long secondValue, Long thirdValue) {
		return objectRelationsRepository
				.findByRequirementsRequirementCodeAndRequirementsSecondValueAndRequirementsThirdValueGreaterThanEqual(
						type.name(), secondValue, thirdValue);
	}

	/**
	 * Will return requirement for specified object type
	 * 
	 * @param targetObject
	 *            - Type of object
	 * @param referenceId
	 *            - Id on the target entity, for example id of an upgrade, or an
	 *            unit
	 * @author Kevin Guanche Darias
	 */
	public List<RequirementInformation> getRequirements(RequirementTargetObject targetObject, Integer referenceId) {
		ObjectRelation objectRelation = getObjectRelation(targetObject, referenceId);
		return returnRequirementOrEmptyList(objectRelation);
	}

	public List<RequirementInformation> findRequirementsByType(RequirementTargetObject targetObject,
			Integer referenceId, RequirementType type) {
		ObjectRelation objectRelation = getObjectRelation(targetObject, referenceId);
		return findRequirementsByType(objectRelation, type);
	}

	public List<RequirementInformation> findRequirementsByType(ObjectRelation objectRelation, RequirementType type) {
		if (objectRelation == null) {
			return new ArrayList<>();
		} else {
			return requirementInformationRepository.findByRelationIdAndRequirementId(objectRelation.getId(),
					type.getValue());
		}
	}

	public ObjectRelation getObjectRelation(RequirementTargetObject targetObject, Integer referenceId) {
		return objectRelationsRepository.findOneByObjectDescriptionAndReferenceId(targetObject.name(), referenceId);
	}

	/**
	 * Will save the requirement information to the database
	 * 
	 * @param requirementsInformation
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void save(RequirementInformation requirementInformation) {
		checkValidObjectRelation(requirementInformation.getRelation());
		requirementInformation.setRelation(objectRelationsRepository.save(requirementInformation.getRelation()));
		requirementInformationRepository.save(requirementInformation);
		List<RequirementInformation> storedRequirementsInformation = requirementInformation.getRelation()
				.getRequirements();
		if (storedRequirementsInformation == null) {
			storedRequirementsInformation = new ArrayList<>();
		}
		storedRequirementsInformation.add(requirementInformation);
		requirementInformation.getRelation().setRequirements(storedRequirementsInformation);

		requirementInformation.setRelation(objectRelationsRepository.save(requirementInformation.getRelation()));
		getRequirements(RequirementTargetObject.UPGRADE, requirementInformation.getRelation().getReferenceId());
	}

	@Transactional
	public void deleteAllObjectRelations(RequirementTargetObject target, Integer referenceId) {
		ObjectRelation objectRelation = getObjectRelation(target, referenceId);
		objectRelationsRepository.delete(objectRelation.getId());
		if (target == RequirementTargetObject.UPGRADE) {
			Integer upgradeLevelRequirementId = requirementRepository
					.findOneByCode(RequirementType.UPGRADE_LEVEL.name()).getId();
			requirementInformationRepository.deleteByRequirementIdAndSecondValue(upgradeLevelRequirementId,
					referenceId.longValue());
		}
	}

	@Transactional(readOnly = false)
	public void deleteRequirementInformation(RequirementInformation requirementInformation) {
		requirementInformationRepository.delete(requirementInformation.getId());
		requirementInformationRepository.flush();
		objectRelationsRepository.flush();
	}

	/**
	 * Gets the human friendly second value description<br />
	 * For example: "{upgrade_name} level some"
	 * 
	 * @param requirementInformation
	 * @return
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
	 * Will find all requirement information for given object type and
	 * requirement id
	 * 
	 * @param objectType
	 * @param requirement
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<RequirementInformation> findByObjectTypeAndRequirementId(RequirementTargetObject objectType,
			RequirementType requirement) {
		return requirementInformationRepository.findByRelationObjectDescriptionAndRequirementId(objectType.name(),
				requirement.getValue());
	}

	/**
	 * Checks if the object relation reference_id exists
	 * 
	 * @param relation
	 *            {@link ObjectRelation}
	 * 
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void checkValidObjectRelation(ObjectRelation relation) {
		if (relation == null) {
			throw new SgtBackendRequirementException("No existe la relación");
		}

		ObjectEntity object = relation.getObject();
		checkValidObject(object);

		WithNameRepository repository = objectEntityBo.findRepository(object);
		if (!repository.exists(relation.getReferenceId())) {
			throw new SgtBackendRequirementException("No se encontró ninguna referencia con id "
					+ relation.getReferenceId() + " para el repositorio " + object.getRepository());
		}

	}

	/**
	 * Checks if the object is valid
	 * 
	 * @param object
	 * @author Kevin Guanche Darias
	 */
	private void checkValidObject(ObjectEntity object) {
		if (object == null) {
			throw new SgtBackendRequirementException("No se ha especificado un tipo de objeto");
		}

		if (object.getRepository() == null) {
			throw new SgtBackendRequirementException("No hay ninguna entidad asociada");
		}

		if (!objectEntityRepository.exists(object.getDescription())) {
			throw new SgtBackendRequirementException("La entidad objeto " + object.getDescription() + " no existe");
		}
	}

	/**
	 * Returns the requirmeent by object relation, but if null, returns empty
	 * list
	 * 
	 * @param objectRelation
	 * @return
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
