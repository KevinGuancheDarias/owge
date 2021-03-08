package com.kevinguanchedarias.owgejava.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;

/**
 *
 * @since 0.10.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface InterceptableSpeedGroupRepository extends JpaRepository<InterceptableSpeedGroup, Integer> {
	public void deleteByUnit(Unit unit);
}
