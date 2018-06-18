package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.kevinguanchedarias.sgtjava.entity.Planet;

public interface PlanetRepository extends WithNameRepository<Planet, Number>, Serializable {
	public Planet findOneByGalaxyIdAndOwnerNotNullOrderByGalaxyId(Integer galaxyId);

	public long countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(Integer galaxyId);

	public List<Planet> findOneByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(Integer galaxyId, Pageable pageable);

	public Planet findOneByIdAndOwnerId(Long planetId, Integer ownerId);

	public List<Planet> findByOwnerId(Integer ownerId);

	public int countByOwnerId(Integer ownerId);

	public List<Planet> findByGalaxyIdAndSectorAndQuadrant(Integer galaxy, Long sector, Long quadrant);
}