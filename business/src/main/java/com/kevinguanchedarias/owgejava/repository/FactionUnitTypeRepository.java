package com.kevinguanchedarias.owgejava.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.FactionUnitType;

/**
 *
 * @since 0.10.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface FactionUnitTypeRepository extends JpaRepository<FactionUnitType, Integer> {
	/**
	 *
	 * @param factionId
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	void deleteByFactionId(Integer factionId);
}
