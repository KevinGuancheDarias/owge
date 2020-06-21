package com.kevinguanchedarias.owgejava.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.ObjectRelationToObjectRelation;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface ObjectRelationToObjectRelationRepository
		extends JpaRepository<ObjectRelationToObjectRelation, Integer> {
	List<ObjectRelationToObjectRelation> findByMasterId(Integer id);

	/**
	 *
	 * @param id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	void deleteByMasterId(Integer id);

	/**
	 *
	 * @param id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	void deleteBySlaveId(Integer id);
}
