package com.kevinguanchedarias.owgejava.business;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

@Service
@Transactional
public class UnlockedRelationBo implements BaseBo<Long, UnlockedRelation, DtoFromEntity<UnlockedRelation>> {
	private static final long serialVersionUID = 8586133814355378376L;

	@Autowired
	private UnlockedRelationRepository repository;

	@Autowired
	private ObjectRelationBo objectRelationBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	public UnlockedRelation findOneByUserIdAndRelationId(Integer userId, Integer relationId) {
		return repository.findOneByUserIdAndRelationId(userId, relationId);
	}

	/**
	 *
	 * @deprecated Use the same method but with {@link ObjectEnum} instead of
	 *             {@link RequirementTargetObject}
	 * @param userId
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public List<UnlockedRelation> findByUserIdAndObjectType(Integer userId, RequirementTargetObject type) {
		return repository.findByUserIdAndRelationObjectDescription(userId, type.name());
	}

	/**
	 *
	 * @param userId
	 * @param type
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<UnlockedRelation> findByUserIdAndObjectType(Integer userId, ObjectEnum type) {
		return repository.findByUserIdAndRelationObjectDescription(userId, type.name());
	}

	/**
	 * Converts a list of unlocked relations into a list of relations
	 *
	 * @param unlockedRelations list of unlocked relations
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

	/**
	 * Unbox to target dto
	 *
	 * @param <E>               Entity class
	 * @param <D>               Dto class
	 * @param dtoClass          Dto class
	 * @param unlockedRelations
	 * @return List of object entities converted to DTO
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public <E, D extends DtoFromEntity<E>> List<D> unboxToTargetDto(Class<D> dtoClass,
			List<UnlockedRelation> unlockedRelations) {
		return dtoUtilService.convertEntireArray(dtoClass, unboxToTargetEntity(unlockedRelations));
	}

	/**
	 *
	 * @param objectRelation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void deleteByRelation(ObjectRelation objectRelation) {
		repository.deleteByRelation(objectRelation);
	}

	/**
	 *
	 * @param user
	 * @param relation
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean isUnlocked(UserStorage user, ObjectRelation relation) {
		return repository.existsByUserAndRelation(user, relation);
	}

	@Override
	public JpaRepository<UnlockedRelation, Long> getRepository() {
		return repository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<DtoFromEntity<UnlockedRelation>> getDtoClass() {
		throw new SgtBackendNotImplementedException("UnlockedRelation doesn't have a dto ... for now =/");
	}

}
