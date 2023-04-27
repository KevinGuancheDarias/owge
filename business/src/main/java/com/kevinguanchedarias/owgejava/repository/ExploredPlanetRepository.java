package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.ExploredPlanet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;

public interface ExploredPlanetRepository extends Serializable, JpaRepository<ExploredPlanet, Number> {
    ExploredPlanet findOneByUserIdAndPlanetId(Integer userId, Long planetId);

    void deleteByUser(UserStorage user);
}
