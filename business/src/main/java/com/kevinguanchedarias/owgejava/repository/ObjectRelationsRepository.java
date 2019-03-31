package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.ObjectRelation;

public interface ObjectRelationsRepository extends JpaRepository<ObjectRelation, Number>, Serializable {
	public ObjectRelation findOneByObjectDescriptionAndReferenceId(String objectEntityCode, Integer refId);

	public List<ObjectRelation> findByRequirementsRequirementCode(String code);

	public List<ObjectRelation> findByRequirementsRequirementCodeAndRequirementsSecondValue(String code,
			Long secondValue);

	public List<ObjectRelation> findByRequirementsRequirementCodeAndRequirementsSecondValueAndRequirementsThirdValueGreaterThanEqual(
			String code, Long secondValue, Long thirdValue);

	public List<ObjectRelation> findByObjectDescriptionAndRequirementsRequirementCode(String name, String name2);
}