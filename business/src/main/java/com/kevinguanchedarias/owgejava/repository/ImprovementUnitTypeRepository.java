package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;

public interface ImprovementUnitTypeRepository extends JpaRepository<ImprovementUnitType, Integer>, Serializable {
	public List<ImprovementUnitType> findByImprovementIdId(Integer id);
}
