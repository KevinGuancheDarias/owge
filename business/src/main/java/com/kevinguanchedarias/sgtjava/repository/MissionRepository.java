package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.Mission.MissionIdAndTerminationDateProjection;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;

public interface MissionRepository extends JpaRepository<Mission, Number>, Serializable {
	public Mission findOneByUserIdAndTypeCode(Integer userId, String type);

	public Mission findOneByUserIdAndTypeCodeAndSourcePlanetId(Integer userId, String type, Long planetId);

	public Mission findByUserIdAndTypeCodeAndMissionInformationValue(Integer userId, String name, Double value);

	@Query("SELECT m.id as id, m.terminationDate as date FROM Mission m WHERE m.report.id = ?1")
	public MissionIdAndTerminationDateProjection findOneByReportId(Long reportId);

	public List<Mission> findByUserIdAndResolvedFalse(Integer userId);

	public List<Mission> findByTargetPlanetInAndResolvedFalseAndUserNot(List<Planet> myPlanets, UserStorage user);
}
