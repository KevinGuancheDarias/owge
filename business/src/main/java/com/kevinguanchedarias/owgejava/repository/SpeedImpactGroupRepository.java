package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface SpeedImpactGroupRepository extends JpaRepository<SpeedImpactGroup, Integer>, Serializable {

}
