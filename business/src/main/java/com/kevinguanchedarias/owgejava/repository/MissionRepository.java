package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Mission.MissionIdAndTerminationDateProjection;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * The interface Mission repository.
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.20
 */
public interface MissionRepository extends JpaRepository<Mission, Long>, Serializable {
    public Mission findOneByUserIdAndTypeCode(Integer userId, String type);

    public Mission findOneByUserIdAndTypeCodeAndSourcePlanetId(Integer userId, String type, Long planetId);

    public Mission findByUserIdAndTypeCodeAndMissionInformationValue(Integer userId, String name, Double value);

    @Query("SELECT m.id as id, m.terminationDate as date FROM Mission m WHERE m.report.id = ?1")
    public MissionIdAndTerminationDateProjection findOneByReportId(Long reportId);

    /**
     * @param userId
     * @param name
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<Mission> findByUserIdAndTypeCodeAndResolvedFalse(Integer userId, String name);

    public List<Mission> findByUserIdAndResolvedFalse(Integer userId);

    public List<Mission> findByTargetPlanetInAndResolvedFalseAndUserNot(List<Planet> myPlanets, UserStorage user);

    public Long countByTargetPlanetIdAndTypeCodeAndResolvedFalse(Long planetId, String type);

    public Long countByTargetPlanetIdAndResolvedFalse(Long planetId);

    /**
     * @param userId
     * @param name
     * @param targetPlanet
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    public List<Mission> findByUserIdAndTypeCodeAndTargetPlanetIdAndResolvedFalse(Integer userId, String name,
                                                                                  Long targetPlanet);

    /**
     * Counts the number of missions that a user has running
     *
     * @param userId
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public Integer countByUserIdAndResolvedFalse(Integer userId);

    /**
     * Finds missions
     *
     * @param date
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    public List<Mission> findByTerminationDateNotNullAndTerminationDateLessThanAndResolvedFalse(Date date);


    /**
     * Exists by termination date between and target planet.
     *
     * @param now     the now
     * @param endDate the endDate
     * @param planet  the planet
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    boolean existsByTerminationDateBetweenAndTargetPlanetAndResolvedFalse(Date now, Date endDate, Planet planet);


    /**
     * Delete by resolved true and termination date less than.
     *
     * @param limitDate the limit date
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    List<Mission> findByResolvedTrueAndTerminationDateLessThan(Date limitDate);

    /**
     * Find by target planet and resolved false and termination date between.
     *
     * @param targetPlanet the target planet
     * @param start        the start
     * @param end          the end
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    void findByTargetPlanetAndResolvedFalseAndTerminationDateBetween(Planet targetPlanet, Date start, Date end);

    List<Mission> findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(List<Planet> myPlanets, UserStorage user);

    Optional<Mission> findOneByResolvedFalseAndTypeCodeAndMissionInformationValue(String typeCode, Double planetId);
}
