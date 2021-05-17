package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface ObtainedUnitRepository extends JpaRepository<ObtainedUnit, Long>, Serializable {
    List<ObtainedUnit> findByMissionId(Long missionId);

    List<ObtainedUnit> findBySourcePlanetIdAndMissionNull(Long planetId);

    /**
     * Finds all user units that are not of a specified mission type
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @Query("SELECT ou FROM ObtainedUnit ou LEFT JOIN ou.mission LEFT JOIN ou.mission.type WHERE ou.user.id = ?1 AND (ou.mission.type.code IS NULL OR ou.mission.type.code <> 'BUILD_UNIT')")
    List<ObtainedUnit> findByUserAndNotBuilding(Integer userId);

    ObtainedUnit findOneByUserIdAndUnitId(Integer userId, Integer unitId);

    ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetId(Integer userId, Integer unitId, Long sourcePlanetId);

    ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndIdNotAndMissionNull(Integer userId, Integer unitId,
                                                                                 Long sourcePlanetId, Long id);

    Long countByUserAndUnit(UserStorage user, Unit unit);

    void deleteByMissionId(Long missionId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    void deleteByUnit(Unit unit);

    @Query("SELECT ou FROM ObtainedUnit ou LEFT JOIN ou.mission LEFT JOIN ou.mission.type WHERE ou.user.id = ?1 AND ou.unit.id = ?2 AND ou.sourcePlanet.id = ?3 AND(ou.mission.id IS NULL OR ou.mission.type.code = 'DEPLOYED') ")
    ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNullOrDeployed(Integer userId,
                                                                                     Integer unitId, Long planetId);

    List<ObtainedUnit> findByUserIdAndSourcePlanetIdAndMissionIdIsNull(Integer userId, Long planetId);

    @Query("SELECT ou FROM ObtainedUnit ou LEFT JOIN ou.mission LEFT JOIN ou.mission.type WHERE 1=1 AND (ou.mission.id IS NULL AND ou.sourcePlanet.id = ?2) OR (ou.targetPlanet.id = ?2 AND ou.mission.id IS NOT NULL AND ou.mission.id != ?1 AND ou.mission.type.code = 'DEPLOYED' ) ")
    List<ObtainedUnit> findByExplorePlanet(Long exploreMissionId, Long planetId);

    List<ObtainedUnit> findBySourcePlanetIdAndMissionIsNull(Long id);

    Long deleteBySourcePlanetIdAndMissionIdNull(Long sourcePlanetId);

    Long countByMission(Mission mission);


    @Query("SELECT SUM(ou.count * ou.unit.energy) FROM ObtainedUnit ou WHERE user = ?1")
    Double computeConsumedEnergyByUser(UserStorage user);

    @Query("SELECT SUM(ou.count) FROM ObtainedUnit ou WHERE user = ?1 AND ou.unit.type = ?2")
    Long countByUserAndUnitType(UserStorage user, UnitType type);

    @Query("SELECT SUM(ou.count) FROM ObtainedUnit ou WHERE user = ?1 AND ou.unit.type.shareMaxCount = ?2")
    Long countByUserAndSharedCountUnitType(UserStorage user, UnitType type);

    @Query("SELECT SUM(utg.value * ou.count) FROM ObtainedUnit ou INNER JOIN ou.unit.improvement.unitTypesUpgrades utg WHERE ou.user = ?1 AND utg.type = ?2")
    Long sumByUserAndImprovementUnitTypeImprovementType(UserStorage user, String name);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNull(Integer userId, Integer unitId,
                                                                           Long planetId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    ObtainedUnit findOneByUserIdAndUnitIdAndTargetPlanetIdAndMissionTypeCode(Integer userId, Integer unitId,
                                                                             Long planetId, String missionCode);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    List<ObtainedUnit> findBySourcePlanetNotNullAndMissionNullAndUserId(Integer userId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    List<ObtainedUnit> findByUnit(Unit unit);

    List<ObtainedUnit> findByUserIdAndTargetPlanetAndMissionTypeCode(Integer userId, Planet targetPlanet,
                                                                     String name);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.1
     */
    List<ObtainedUnit> findByMissionIdIn(List<Long> missionIds);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.4
     */
    List<ObtainedUnit> findByTargetPlanetIdAndMissionTypeCode(Long id, String missionType);

    /**
     * finds units according to #316
     *
     * @param referenceRationalTime from 0 to 1
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     */
    @Query("SELECT ou FROM ObtainedUnit ou WHERE ou.targetPlanet.id = ?1 AND ou.mission IS NOT NULL AND ou.mission.type.code IN (?3) AND ou.mission.requiredTime * ?2  < TIME_TO_SEC(TIMEDIFF(?4, ou.mission.startingDate))  ")
    List<ObtainedUnit> findByTargetPlanetIdWhereReferencePercentageTimePassed(Long planetId,
                                                                              Double referenceRationalTime, List<String> allowedMissions, Date nowReference);

    /**
     * @param alliance or null
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.5
     */
    @Query("SELECT case when count(ou)> 0 then true else false end FROM ObtainedUnit ou WHERE ou.targetPlanet.id = ?3 AND ou.mission.type.code = 'DEPLOYED' AND ou.user.id != ?1 AND (ou.user.alliance IS NULL OR ?2 IS NULL OR ou.user.alliance != ?2)")
    boolean areUnitsInvolved(Integer userId, Alliance alliance, Long relatedPlanetId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.10
     */
    int countByUserIdAndSourcePlanetIdAndMissionIsNull(Integer userId, Long planetId);

    @Query("SELECT case when count(ou)> 0 then true else false end " +
            "FROM ObtainedUnit ou LEFT JOIN ou.mission LEFT JOIN ou.mission.type " +
            "WHERE ou.user = ?1 AND ou.unit = ?2 AND (ou.mission IS NULL OR ou.mission.type.code != 'BUILD_UNIT') ")
    boolean isBuiltUnit(UserStorage user, Unit unit);
}
