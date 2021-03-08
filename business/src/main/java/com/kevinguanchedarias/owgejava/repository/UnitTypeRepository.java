package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import com.kevinguanchedarias.owgejava.entity.UnitType;


public interface UnitTypeRepository extends WithNameRepository<UnitType, Integer>, Serializable {

	/**
	 * Exists by units type id boolean.
	 *
	 * @param id the id
	 * @return the boolean
	 * @since 0.9.20
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	boolean existsByUnitsTypeId(Integer id);
}