package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.MissionType;

public interface MissionTypeRepository extends JpaRepository<MissionType, Number>, Serializable {
	public MissionType findOneByCode(String code);
}
