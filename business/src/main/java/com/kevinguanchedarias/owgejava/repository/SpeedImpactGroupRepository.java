package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;

public interface SpeedImpactGroupRepository extends JpaRepository<SpeedImpactGroup, Number>, Serializable {

}
