package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.kevinguanchedarias.owgejava.entity.Planet;

public interface PlanetRepository extends WithNameRepository<Planet, Long>, Serializable {
	public Planet findOneByGalaxyIdAndOwnerNotNullOrderByGalaxyId(Integer galaxyId);

	public long countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(Integer galaxyId);

	public long countByOwnerIsNullAndSpecialLocationIsNull();

	public List<Planet> findOneByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(Integer galaxyId, Pageable pageable);

	public List<Planet> findOneByOwnerIsNullAndSpecialLocationIsNull(Pageable pageable);

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
	 * @param galaxyId
	 * @since 0.9.14
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void deleteByGalaxyId(Integer galaxyId);

}