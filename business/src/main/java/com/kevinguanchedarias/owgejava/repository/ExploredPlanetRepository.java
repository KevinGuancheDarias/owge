package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.ExploredPlanet;

public interface ExploredPlanetRepository extends Serializable, JpaRepository<ExploredPlanet, Number> {
	public ExploredPlanet findOneByUserIdAndPlanetId(Integer userId, Long planetId);
}
