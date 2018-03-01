package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.ExploredPlanet;

public interface ExploredPlanetRepository extends Serializable, JpaRepository<ExploredPlanet, Number> {
	public ExploredPlanet findOneByUserIdAndPlanetId(Integer userId, Long planetId);
}
