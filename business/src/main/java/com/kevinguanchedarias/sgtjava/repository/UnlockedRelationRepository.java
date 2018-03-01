package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.UnlockedRelation;

public interface UnlockedRelationRepository extends JpaRepository<UnlockedRelation, Number>, Serializable {
	public UnlockedRelation findOneByUserIdAndRelationId(Integer userId, Integer relationId);

	public List<UnlockedRelation> findByUserIdAndRelationObjectDescription(Integer userId, String objectType);
}
