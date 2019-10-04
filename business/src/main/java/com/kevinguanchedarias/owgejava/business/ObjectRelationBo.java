package com.kevinguanchedarias.owgejava.business;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.ObjectType;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendRequirementException;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.WithNameRepository;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;

@Component
public class ObjectRelationBo implements BaseBo<ObjectRelation> {
	private static final long serialVersionUID = -8660185836978327225L;
	private static final Logger LOG = Logger.getLogger(RequirementInformationBo.class);

	@Autowired
	private ObjectEntityBo objectEntityBo;

	@Autowired
	private ObjectRelationsRepository objectRelationsRepository;

	/**
	 * @deprecated Use {@link ObjectEnum} instead as first parameter
	 * @param type
	 * @param referenceId
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public ObjectRelation findOneByObjectTypeAndReferenceId(RequirementTargetObject type, Integer referenceId) {
		return findOneByObjectTypeAndReferenceId(ObjectEnum.valueOf(type.name()), referenceId);
	}

	/**
	 * 
	 * @param type
	 * @param referenceId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectRelation findOneByObjectTypeAndReferenceId(ObjectEnum type, Integer referenceId) {
		return objectRelationsRepository.findOneByObjectDescriptionAndReferenceId(type.name(), referenceId);
	}

	/**
	 * Finds one or throws {@link NotFoundException}
	 * 
	 * @param type
	 * @param referenceId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectRelation findOneByObjectTypeAndReferenceIdOrDie(ObjectEnum type, Integer referenceId) {
		ObjectRelation objectRelation = findOneByObjectTypeAndReferenceId(type, referenceId);
		if (objectRelation == null) {
			NotFoundException notFoundException = NotFoundException
					.fromAffected(SpringRepositoryUtil.findEntityClass(getRepository()), referenceId);
			throw notFoundException.addExtraDeveloperHint("Remember that the check is a pair between type ("
					+ type.name() + ") and referenceId (" + referenceId + ")");
		}
		return objectRelation;
	}

	/**
	 * Extracts the object target object entity from the relation
	 * 
	 * @param relation
	 * @return An instance of: Upgrade.class , Unit.class depending on to what
	 *         connection is doing the relation
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings("unchecked")
	public <E> E unboxObjectRelation(ObjectRelation relation) {
		WithNameRepository<E, Number> repository = objectEntityBo.findRepository(relation.getObject());

		return repository.findOne(relation.getReferenceId());
	}

	@SuppressWarnings("unchecked")
	public <E> List<E> unboxObjectRelation(List<ObjectRelation> relations) {
		List<E> retVal = new ArrayList<>();
		if (!CollectionUtils.isEmpty(relations)) {
			WithNameRepository<E, Number> repository = objectEntityBo.findRepository(relations.get(0).getObject());
			relations.stream().forEach(current -> retVal.add(repository.findOne(current.getReferenceId())));
		}
		return retVal;
	}

	/**
	 * Finds by type and ref id
	 * 
	 * @param objectEnum
	 * @param referenceId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectRelation findOne(ObjectEnum objectEnum, Integer referenceId) {
		return objectRelationsRepository.findOneByObjectDescriptionAndReferenceId(objectEnum.name(), referenceId);
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public ObjectRelation findObjectRelationOrCreate(ObjectEnum target, Integer referenceId) {
		ObjectRelation objectRelation = findOne(target, referenceId);
		if (objectRelation != null) {
			return objectRelation;
		} else {
			LOG.debug("No object relation of type " + target.name() + " with refId " + referenceId
					+ " exists in the target db, will create one");
			objectRelation = new ObjectRelation();
			objectRelation.setObject(objectEntityBo.findByDescription(target));
			objectRelation.setReferenceId(referenceId);
			return save(objectRelation);
		}

	}

	/**
	 * 
	 * @param type
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObjectRelation> findObjectRelationsHavingRequirementType(RequirementTypeEnum type) {
		return objectRelationsRepository.findByRequirementsRequirementCode(type.name());
	}

	/**
	 * 
	 * @param type
	 * @param requirementType
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObjectRelation> findObjectRelationsOfTypeHavingRequirementType(ObjectType type,
			RequirementTypeEnum requirementType) {
		return objectRelationsRepository.findByObjectDescriptionAndRequirementsRequirementCode(type.name(),
				requirementType.name());
	}

	/**
	 * 
	 * @param type
	 * @param secondValue
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObjectRelation> findByRequirementTypeAndSecondValue(RequirementTypeEnum type, Long secondValue) {
		return objectRelationsRepository.findByRequirementsRequirementCodeAndRequirementsSecondValue(type.name(),
				secondValue);
	}

	/**
	 * Finds by type, secondValue, and where thirdValue is greater or equal to x<br>
	 * Example resultant SQL: WHERE type = '$type' AND secondValue = '$secondValue'
	 * AND thirdValue >= '$thidValue'
	 * 
	 * @param type
	 * @param secondValue
	 * @param thirdValue
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObjectRelation> findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(RequirementTypeEnum type,
			Long secondValue, Long thirdValue) {
		return objectRelationsRepository
				.findByRequirementsRequirementCodeAndRequirementsSecondValueAndRequirementsThirdValueGreaterThanEqual(
						type.name(), secondValue, thirdValue);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void checkValid(ObjectRelation relation) {
		if (relation == null) {
			throw new SgtBackendRequirementException("No existe la relación");
		}

		ObjectEntity object = relation.getObject();
		objectEntityBo.checkValid(object);

		WithNameRepository repository = objectEntityBo.findRepository(object);
		if (!repository.exists(relation.getReferenceId())) {
			throw new SgtBackendRequirementException("No se encontró ninguna referencia con id "
					+ relation.getReferenceId() + " para el repositorio " + object.getRepository());
		}

	}

	@Override
	public JpaRepository<ObjectRelation, Number> getRepository() {
		return objectRelationsRepository;
	}
}
