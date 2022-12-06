package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Galaxy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;
import java.util.List;

public interface GalaxyRepository extends WithNameRepository<Galaxy, Integer>, Serializable {

    @Query("SELECT g FROM Galaxy  g JOIN g.planets p WHERE g.id = ?1 AND p.owner IS NOT NULL")
    List<Galaxy> hasPlayers(Integer galaxyId, Pageable pageable);
}
