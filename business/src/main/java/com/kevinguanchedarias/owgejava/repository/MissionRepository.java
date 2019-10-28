package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Mission.MissionIdAndTerminationDateProjection;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface MissionRepository extends JpaRepository<Mission, Long>, Serializable {
	public Mission findOneByUserIdAndTypeCode(Integer userId, String type);

	public Mission findOneByUserIdAndTypeCodeAndSourcePlanetId(Integer userId, String type, Long planetId);

	public Mission findByUserIdAndTypeCodeAndMissionInformationValue(Integer userId, String name, Double value);

	@Query("SELECT m.id as id, m.terminationDate as date FROM Mission m WHERE m.report.id = ?1")
	public MissionIdAndTerminationDateProjection findOneByReportId(Long reportId);

	public List<Mission> findByUserIdAndResolvedFalse(Integer userId);

	public List<Mission> findByTargetPlanetInAndResolvedFalseAndUserNot(List<Planet> myPlanets, UserStorage user);

	public Long countByTargetPlanetIdAndTypeCodeAndResolvedFalse(Long planetId, String type);

	public Long countByTargetPlanetIdAndResolvedFalse(Long planetId);

	/**
	 * @param userId
	 * @param name
	 * @param targetPlanet
	 * @return
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<Mission> findByUserIdAndTypeCodeAndTargetPlanetIdAndResolvedFalse(Integer userId, String name,
			Long targetPlanet);
}
