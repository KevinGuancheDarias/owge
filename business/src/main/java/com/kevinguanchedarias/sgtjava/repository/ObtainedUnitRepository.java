package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;

public interface ObtainedUnitRepository extends JpaRepository<ObtainedUnit, Number>, Serializable {
	public List<ObtainedUnit> findByMissionId(Long missionId);

	public List<ObtainedUnit> findBySourcePlanetId(Long planetId);

	public List<ObtainedUnit> findBySourcePlanetIdAndMissionNull(Long planetId);

	public ObtainedUnit findOneByUserIdAndUnitId(Integer userId, Integer unitId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetId(Integer userId, Integer unitId, Long sourcePlanetId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndIdNot(Integer userId, Integer unitId,
			Long sourcePlanetId, Long id);

	public Long countByUserIdAndUnitId(Integer userId, Integer unitId);

	public Long deleteByMissionId(Long missionId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIdIsNull(Integer userId, Integer unitId,
			Long planetId);

	public List<ObtainedUnit> findByUserIdAndSourcePlanetIdAndMissionIdIsNull(Integer userId, Long planetId);

	public List<ObtainedUnit> findBySourcePlanetIdAndMissionIsNull(Long id);

	public List<ObtainedUnit> findByTargetPlanetIdAndMissionIdNotNullAndMissionIdNot(Long planetId, Long missionId);

	public ObtainedUnit findOneByMission(Mission mission);

	public Long deleteBySourcePlanetIdAndMissionIdNull(Long sourcePlanetId);

	public Long countByMission(Mission mission);

	public Long countByUserIdAndSourcePlanetId(Integer userId, Long planetId);
}
