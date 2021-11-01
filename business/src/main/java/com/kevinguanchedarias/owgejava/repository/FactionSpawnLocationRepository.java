package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.FactionSpawnLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FactionSpawnLocationRepository extends JpaRepository<FactionSpawnLocation, Integer> {

    void deleteByFactionId(Integer factionId);

    @Query("SELECT f.galaxy.id FROM FactionSpawnLocation f WHERE f.faction = ?1")
    List<Integer> findSpawnGalaxiesByFaction(Faction faction);

    List<FactionSpawnLocation> findByFactionId(int factionId);
}
