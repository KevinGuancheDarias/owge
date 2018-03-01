package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.SpeedImpactGroup;

public interface SpeedImpactGroupRepository extends JpaRepository<SpeedImpactGroup, Number>, Serializable {

}
