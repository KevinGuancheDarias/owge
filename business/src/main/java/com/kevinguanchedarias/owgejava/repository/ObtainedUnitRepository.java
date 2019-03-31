package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface ObtainedUnitRepository extends JpaRepository<ObtainedUnit, Number>, Serializable {
	public List<ObtainedUnit> findByMissionId(Long missionId);

	public List<ObtainedUnit> findBySourcePlanetId(Long planetId);

	public List<ObtainedUnit> findBySourcePlanetIdAndMissionNull(Long planetId);

	public ObtainedUnit findOneByUserIdAndUnitId(Integer userId, Integer unitId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetId(Integer userId, Integer unitId, Long sourcePlanetId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndIdNotAndMissionNull(Integer userId, Integer unitId,
			Long sourcePlanetId, Long id);

	public Long countByUserIdAndUnitId(Integer userId, Integer unitId);

	public Long deleteByMissionId(Long missionId);

	@Query("SELECT ou FROM ObtainedUnit ou LEFT JOIN ou.mission LEFT JOIN ou.mission.type WHERE ou.user.id = ?1 AND ou.unit.id = ?2 AND ou.sourcePlanet.id = ?3 AND(ou.mission.id IS NULL OR ou.mission.type.code = 'DEPLOYED') ")
	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNullOrDeployed(Integer userId,
			Integer unitId, Long planetId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIdIsNull(Integer userId, Integer unitId,
			Long planetId);

	public List<ObtainedUnit> findByUserIdAndSourcePlanetIdAndMissionIdIsNull(Integer userId, Long planetId);

	public List<ObtainedUnit> findBySourcePlanetIdAndMissionIsNull(Long id);

	public List<ObtainedUnit> findByTargetPlanetIdAndMissionIdNotNullAndMissionIdNot(Long planetId, Long missionId);

	public ObtainedUnit findOneByMission(Mission mission);

	public Long deleteBySourcePlanetIdAndMissionIdNull(Long sourcePlanetId);

	public Long countByMission(Mission mission);

	public Long countByUserIdAndSourcePlanetId(Integer userId, Long planetId);

	@Query("SELECT SUM(ou.count * ou.unit.energy) FROM ObtainedUnit ou WHERE user = ?1")
	public Double computeConsumedEnergyByUser(UserStorage user);

	@Query("SELECT SUM(ou.count) FROM ObtainedUnit ou WHERE user = ?1 AND ou.unit.type = ?2")
	public Long countByUserAndUnitType(UserStorage user, UnitType type);

	@Query("SELECT SUM(utg.value * ou.count) FROM ObtainedUnit ou INNER JOIN ou.unit.improvement.unitTypesUpgrades utg WHERE ou.user = ?1 AND utg.type = ?2")
	public Long sumByUserAndImprovementUnitTypeImprovementType(UserStorage user, String name);

}
