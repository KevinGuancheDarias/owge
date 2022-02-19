package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.Optional;

public interface MissionTypeRepository extends JpaRepository<MissionType, Number>, Serializable {
    Optional<MissionType> findOneByCode(String code);
}
