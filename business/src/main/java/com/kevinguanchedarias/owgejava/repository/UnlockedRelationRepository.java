package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;

public interface UnlockedRelationRepository extends JpaRepository<UnlockedRelation, Long>, Serializable {
	public UnlockedRelation findOneByUserIdAndRelationId(Integer userId, Integer relationId);

	public List<UnlockedRelation> findByUserIdAndRelationObjectDescription(Integer userId, String objectType);
}
