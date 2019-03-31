package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.MissionType;

public interface MissionTypeRepository extends JpaRepository<MissionType, Number>, Serializable {
	public MissionType findOneByCode(String code);
}
