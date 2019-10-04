package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;

public interface RequirementInformationRepository extends JpaRepository<RequirementInformation, Number>, Serializable {
	public List<RequirementInformation> findByRelationId(Integer relationId);

	public List<RequirementInformation> findByRelationIdAndRequirementId(Integer relationId, Integer requirementId);

	public List<RequirementInformation> findByRelationObjectDescriptionAndRequirementId(String objectDescription,
			Integer requirementId);

	public void deleteByRequirementIdAndSecondValue(Integer requirementId, Long secondValue);
}
