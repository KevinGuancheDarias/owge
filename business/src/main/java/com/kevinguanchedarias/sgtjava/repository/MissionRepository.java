package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.Mission;

public interface MissionRepository extends JpaRepository<Mission, Number>, Serializable {
	public Mission findOneByUserIdAndTypeCode(Integer userId, String type);

	public Mission findOneByUserIdAndTypeCodeAndSourcePlanetId(Integer userId, String type, Long planetId);

	public Mission findByUserIdAndTypeCodeAndMissionInformationValue(Integer userId, String name, Double value);
}
