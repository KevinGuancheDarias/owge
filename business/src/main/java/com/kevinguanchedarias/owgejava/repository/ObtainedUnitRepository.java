package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface ObtainedUnitRepository extends JpaRepository<ObtainedUnit, Long>, Serializable {
	public List<ObtainedUnit> findByMissionId(Long missionId);

	public List<ObtainedUnit> findBySourcePlanetId(Long planetId);

	/**
	 *
	 * @param id
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public List<ObtainedUnit> findByTargetPlanetId(Long id);

	public List<ObtainedUnit> findBySourcePlanetIdAndMissionNull(Long planetId);

	/**
	 * Finds all user units that are not of a specified mission type
	 *
	 * @param userId
	 * @param code
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Query("SELECT ou FROM ObtainedUnit ou LEFT JOIN ou.mission LEFT JOIN ou.mission.type WHERE ou.user.id = ?1 AND (ou.mission.type.code IS NULL OR ou.mission.type.code <> 'BUILD_UNIT')")
	public List<ObtainedUnit> findByUserAndNotBuilding(Integer userId);

	public ObtainedUnit findOneByUserIdAndUnitId(Integer userId, Integer unitId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetId(Integer userId, Integer unitId, Long sourcePlanetId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndIdNotAndMissionNull(Integer userId, Integer unitId,
			Long sourcePlanetId, Long id);

	public Long countByUserIdAndUnitId(Integer userId, Integer unitId);

	public Long deleteByMissionId(Long missionId);

	/**
	 *
	 * @param unit
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void deleteByUnit(Unit unit);

	@Query("SELECT ou FROM ObtainedUnit ou LEFT JOIN ou.mission LEFT JOIN ou.mission.type WHERE ou.user.id = ?1 AND ou.unit.id = ?2 AND ou.sourcePlanet.id = ?3 AND(ou.mission.id IS NULL OR ou.mission.type.code = 'DEPLOYED') ")
	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNullOrDeployed(Integer userId,
			Integer unitId, Long planetId);

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIdIsNull(Integer userId, Integer unitId,
			Long planetId);

	public List<ObtainedUnit> findByUserIdAndSourcePlanetIdAndMissionIdIsNull(Integer userId, Long planetId);

	@Query("SELECT ou FROM ObtainedUnit ou LEFT JOIN ou.mission LEFT JOIN ou.mission.type WHERE 1=1 AND (ou.mission.id IS NULL AND ou.sourcePlanet.id = ?2) OR (ou.targetPlanet.id = ?2 AND ou.mission.id IS NOT NULL AND ou.mission.id != ?1 AND ou.mission.type.code = 'DEPLOYED' ) ")
	public List<ObtainedUnit> findByExplorePlanet(Long exploreMissionId, Long planetId);

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

	@Query("SELECT SUM(ou.count) FROM ObtainedUnit ou WHERE user = ?1 AND ou.unit.type.shareMaxCount = ?2")
	public Long countByUserAndSharedCountUnitType(UserStorage user, UnitType type);

	@Query("SELECT SUM(utg.value * ou.count) FROM ObtainedUnit ou INNER JOIN ou.unit.improvement.unitTypesUpgrades utg WHERE ou.user = ?1 AND utg.type = ?2")
	public Long sumByUserAndImprovementUnitTypeImprovementType(UserStorage user, String name);

	/**
	 *
	 * @param userId
	 * @param unitId
	 * @param planetId
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "5000") })
	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNull(Integer userId, Integer unitId,
			Long planetId);

	/**
	 *
	 * @param userId
	 * @param unitId
	 * @param planetId
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "5000") })
	public ObtainedUnit findOneByUserIdAndUnitIdAndTargetPlanetIdAndMissionTypeCode(Integer userId, Integer unitId,
			Long planetId, String missionCode);

	/**
	 *
	 * @param userId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> findBySourcePlanetNotNullAndMissionNullAndUserId(Integer userId);

	@Query("SELECT ou FROM ObtainedUnit ou WHERE ou.mission.id = :missionId")
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({ @QueryHint(name = "javax.persistence.lock.timeout", value = "5000") })
	public List<ObtainedUnit> findLockedByMissionId(Long missionId);

	/**
	 *
	 * @param unit
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> findByUnit(Unit unit);

	/**
	 *
	 * @param userId
	 * @param targetPlanet
	 * @param name
	 * @return
	 */
	public List<ObtainedUnit> findByUserIdAndTargetPlanetAndMissionTypeCode(Integer userId, Planet targetPlanet,
			String name);

	/**
	 *
	 * @param missionIds
	 * @return
	 * @since 0.9.1
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> findByMissionIdIn(List<Long> missionIds);

	/**
	 *
	 * @param id
	 * @param missionType
	 * @return
	 * @since 0.9.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> findByTargetPlanetIdAndMissionTypeCode(Long id, String missionType);

	/**
	 * finds units according to #316
	 *
	 * @param planetId
	 * @param referenceRationalTime from 0 to 1
	 * @return
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Query("SELECT ou FROM ObtainedUnit ou WHERE ou.targetPlanet.id = ?1 AND ou.mission IS NOT NULL AND ou.mission.type.code IN (?3) AND ou.mission.requiredTime * ?2  < TIME_TO_SEC(TIMEDIFF(?4, ou.mission.startingDate))  ")
	public List<ObtainedUnit> findByTargetPlanetIdWhereReferencePercentageTimePassed(Long planetId,
			Double referenceRationalTime, List<String> allowedMissions, Date nowReference);

	/**
	 *
	 * @param userId
	 * @param alliance or null
	 * @param id3
	 * @return
	 * @since 0.9.5
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Query("SELECT case when count(ou)> 0 then true else false end FROM ObtainedUnit ou WHERE ou.targetPlanet.id = ?3 AND ou.mission.type.code = 'DEPLOYED' AND ou.user.id != ?1 AND (ou.user.alliance IS NULL OR ?2 IS NULL OR ou.user.alliance != ?2)")
	public boolean areUnitsInvolved(Integer userId, Alliance alliance, Long relatedPlanetId);

	/**
	 *
	 * @param userId
	 * @param planetId
	 * @return
	 * @since 0.9.10
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public int countByUserIdAndSourcePlanetIdAndMissionIsNull(Integer userId, Long planetId);

}
