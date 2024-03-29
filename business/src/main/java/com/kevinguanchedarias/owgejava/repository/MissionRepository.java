package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Mission.MissionIdAndTerminationDateProjection;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The interface Mission repository.
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.20
 */
public interface MissionRepository extends JpaRepository<Mission, Long>, Serializable {
    Mission findOneByUserIdAndTypeCode(Integer userId, String type);


    Mission findByUserIdAndTypeCodeAndMissionInformationValue(Integer userId, String name, Double value);

    @Query("SELECT m.id as id, m.terminationDate as date FROM Mission m WHERE m.report.id = ?1")
    MissionIdAndTerminationDateProjection findOneByReportId(Long reportId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    List<Mission> findByUserIdAndTypeCodeAndResolvedFalse(Integer userId, String name);

    List<Mission> findByUserIdAndResolvedFalse(Integer userId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    List<Mission> findByUserIdAndTypeCodeAndTargetPlanetIdAndResolvedFalse(Integer userId, String name,
                                                                           Long targetPlanet);

    /**
     * Counts the number of missions that a user has running
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    Integer countByUserIdAndResolvedFalse(Integer userId);

    @Query("SELECT m FROM Mission m WHERE m.terminationDate IS NOT NULL AND  m.terminationDate < ?1 AND m.resolved = false ")
    List<Mission> findHangMissions(LocalDateTime terminationDate);

    /**
     * Delete by resolved true and termination date less than.
     *
     * @param limitDate the limit date
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    List<Mission> findByResolvedTrueAndTerminationDateLessThan(LocalDateTime limitDate);

    List<Mission> findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(List<Planet> myPlanets, UserStorage user);

    Optional<Mission> findOneByResolvedFalseAndTypeCodeAndMissionInformationValue(String typeCode, Double planetId);

    void deleteByUserAndTypeCodeIn(UserStorage user, List<String> missionTypes);

    @Query("UPDATE Mission m SET m.report = ?1 ")
    @Modifying
    void updateReportId(MissionReport report);

    List<Mission> findByUser(UserStorage user);
}
