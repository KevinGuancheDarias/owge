package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import com.kevinguanchedarias.owgejava.entity.Planet;

public interface PlanetRepository extends WithNameRepository<Planet, Long>, Serializable {
	public Planet findOneByGalaxyIdAndOwnerNotNullOrderByGalaxyId(Integer galaxyId);

	public long countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(Integer galaxyId);

	public List<Planet> findOneByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(Integer galaxyId, Pageable pageable);

	public Planet findOneByIdAndOwnerId(Long planetId, Integer ownerId);

	public List<Planet> findByOwnerId(Integer ownerId);

	public int countByOwnerId(Integer ownerId);

	public List<Planet> findByGalaxyIdAndSectorAndQuadrant(Integer galaxy, Long sector, Long quadrant);

	public Planet findOneByIdAndHomeTrue(Long planetId);

	/**
	 *
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @param galaxyId
	 * @return
	 */
	public List<Planet> findByGalaxyIdAndOwnerNotNull(Integer galaxyId);

	/**
	 *
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @param specialLocationId
	 * @return
	 */
	public Planet findOneBySpecialLocationId(Integer specialLocationId);

	/**
	 *
	 * @param id
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Query("SELECT p FROM Planet p WHERE id = :id")
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "5000") })
	public Planet findLockedById(Long id);
}