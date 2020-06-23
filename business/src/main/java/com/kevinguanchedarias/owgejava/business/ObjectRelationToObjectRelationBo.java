package com.kevinguanchedarias.owgejava.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelationToObjectRelation;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
public class ObjectRelationToObjectRelationBo
		implements BaseBo<Integer, ObjectRelationToObjectRelation, DtoFromEntity<ObjectRelationToObjectRelation>> {
	private static final long serialVersionUID = 9174432635190297289L;

	@Autowired
	@Lazy
	private ObjectRelationToObjectRelationRepository repository;

	@Override
	public Class<DtoFromEntity<ObjectRelationToObjectRelation>> getDtoClass() {
		throw new SgtBackendNotImplementedException("No DTO for now");
	}

	@Override
	public JpaRepository<ObjectRelationToObjectRelation, Integer> getRepository() {
		return repository;
	}

	/**
	 *
	 * @param relationId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObjectRelationToObjectRelation> findByMasterId(Integer relationId) {
		return repository.findByMasterId(relationId);
	}

	/**
	 *
	 * @param relationId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void deleteBySlaveId(Integer relationId) {
		repository.deleteBySlaveId(relationId);
	}

}
