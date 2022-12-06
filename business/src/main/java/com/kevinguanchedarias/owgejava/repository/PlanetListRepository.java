package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.PlanetList;
import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
public interface PlanetListRepository extends JpaRepository<PlanetList, PlanetUser> {

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    List<PlanetList> findByPlanetUserUserId(Integer userId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.8
     */
    @Query("SELECT pl.planetUser.user.id FROM PlanetList pl WHERE pl.planetUser.planet = ?1")
    List<Integer> findUserIdByPlanetListPlanet(Planet planetU);

}
