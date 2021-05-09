package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.CriticalAttackEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriticalAttackEntryRepository extends JpaRepository<CriticalAttackEntry, Integer> {

    void deleteByCriticalAttackId(Integer id);
}
