package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.FactionUnitType;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 */
public interface FactionUnitTypeRepository extends JpaRepository<FactionUnitType, Integer> {
    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    void deleteByFactionId(Integer factionId);

    Optional<FactionUnitType> findOneByFactionAndUnitType(Faction faction, UnitType type);
}
