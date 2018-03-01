package com.kevinguanchedarias.sgtjava.business;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.entity.ObjectRelation;
import com.kevinguanchedarias.sgtjava.entity.UnlockedRelation;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.repository.UnlockedRelationRepository;

@Service
@Transactional
public class UnlockedRelationBo implements BaseBo<UnlockedRelation> {
	private static final long serialVersionUID = 8586133814355378376L;

	@Autowired
	private UnlockedRelationRepository repository;

	@Autowired
	private ObjectRelationBo objectRelationBo;

	@Override
	public JpaRepository<UnlockedRelation, Number> getRepository() {
		return repository;
	}

	public UnlockedRelation findOneByUserIdAndRelationId(Integer userId, Integer relationId) {
		return repository.findOneByUserIdAndRelationId(userId, relationId);
	}

	public List<UnlockedRelation> findByUserIdAndObjectType(Integer userId, RequirementTargetObject type) {
		return repository.findByUserIdAndRelationObjectDescription(userId, type.name());
	}

	/**
	 * Converts a list of unlocked relations into a list of relations
	 * 
	 * @param unlockedRelations
	 *            list of unlocked relations
	 * @return list of relations
	 * @author Kevin Guanche Darias
	 */
	public List<ObjectRelation> unboxUnlockedRelationList(List<UnlockedRelation> unlockedRelations) {
		List<ObjectRelation> relations = new ArrayList<>();
		unlockedRelations.stream().forEach(unlockedRelation -> relations.add(unlockedRelation.getRelation()));
		return relations;
	}

	/**
	 * Unbox to target entity, for example will return a list of Units
	 * 
	 * @param unlockedRelations
	 * @return List of Object Entities
	 * @author Kevin Guanche Darias
	 */
	public <E> List<E> unboxToTargetEntity(List<UnlockedRelation> unlockedRelations) {
		return objectRelationBo.unboxObjectRelation(unboxUnlockedRelationList(unlockedRelations));
	}
}
