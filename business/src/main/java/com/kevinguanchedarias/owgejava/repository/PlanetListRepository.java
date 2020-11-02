package com.kevinguanchedarias.owgejava.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.PlanetList;
import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface PlanetListRepository extends JpaRepository<PlanetList, PlanetUser> {

	/**
	 *
	 * @param userId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	List<PlanetList> findByPlanetUserUserId(Integer userId);

	/**
	 *
	 * @param planetU
	 * @return
	 * @since 0.9.8
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Query("SELECT pl.planetUser.user.id FROM PlanetList pl WHERE pl.planetUser.planet = ?1")
	List<Integer> findUserIdByPlanetListPlanet(Planet planetU);

}
