package com.kevinguanchedarias.sgtjava.business;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.sgtjava.entity.ObjectRelation;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendEmptyList;
import com.kevinguanchedarias.sgtjava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.sgtjava.repository.WithNameRepository;

@Component
public class ObjectRelationBo implements BaseBo<ObjectRelation> {
	private static final long serialVersionUID = -8660185836978327225L;

	@Autowired
	private ObjectEntityBo objectEntityBo;

	@Autowired
	private ObjectRelationsRepository objectRelationsRepository;

	public ObjectRelation findOneByObjectTypeAndReferenceId(RequirementTargetObject type, Integer referenceId) {
		return objectRelationsRepository.findOneByObjectDescriptionAndReferenceId(type.name(), referenceId);
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
		if (relations == null || relations.isEmpty()) {
			throw new SgtBackendEmptyList("List of relations can't be empty");
		}
		WithNameRepository<E, Number> repository = objectEntityBo.findRepository(relations.get(0).getObject());
		relations.stream().forEach(current -> retVal.add(repository.findOne(current.getReferenceId())));
		return retVal;
	}

	@Override
	public JpaRepository<ObjectRelation, Number> getRepository() {
		return objectRelationsRepository;
	}
}
