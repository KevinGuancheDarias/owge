package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.ImprovementUnitType;

public interface ImprovementRepository extends JpaRepository<ImprovementUnitType, Integer>, Serializable {
	public List<ImprovementUnitType> findByImprovementIdId(Integer id);
}
