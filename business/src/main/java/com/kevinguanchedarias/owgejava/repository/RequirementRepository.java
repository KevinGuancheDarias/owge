package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.Requirement;

public interface RequirementRepository extends JpaRepository<Requirement, Integer>, Serializable {
	public Requirement findOneByCode(String code);
}
