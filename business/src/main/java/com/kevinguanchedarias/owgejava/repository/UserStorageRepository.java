package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface UserStorageRepository extends JpaRepository<UserStorage, Integer>, Serializable {
	public UserStorage findOneByIdAndFactionId(Integer userId, Integer factionId);

	public List<UserStorage> findAllByOrderByPointsDesc();

	@Modifying
	@Query("UPDATE UserStorage u SET u.points = u.points + :points WHERE u = :user")
	public void addPointsToUser(@Param("user") UserStorage user, @Param("points") Double points);

	public UserStorage findOneByMissions(Mission mission);

	/**
	 * @param oldAlliance
	 * @param newAlliance
	 * @return
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Query("UPDATE UserStorage u SET u.alliance = :new WHERE u.alliance = :old")
	@Modifying
	public void defineAllianceByAllianceId(@Param("old") Alliance oldAlliance, @Param("new") Alliance newAlliance);

	/**
	 * Adds resources to the user
	 *
	 * @param user
	 * @param now
	 * @param primary
	 * @param secondary
	 * @since 0.9.12
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Query("UPDATE UserStorage u SET u.lastAction = ?2, u.primaryResource = u.primaryResource + ?3, u.secondaryResource = u.secondaryResource + ?4 WHERE u = ?1")
	@Modifying
	public void addResources(UserStorage user, Date now, Double primary, Double secondary);

	/**
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.9.14
	 */
	int countByAlliance(Alliance alliance);

	/**
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.9.16
	 */
	@Query("SELECT id FROM #{#entityName}")
	List<Integer> findAllIds();

	List<UserStorage> findByLastMultiAccountCheckLessThanOrLastMultiAccountCheckIsNullOrderByLastMultiAccountCheckAsc(LocalDateTime date, Pageable page);

	@Query("SELECT us.banned FROM UserStorage us WHERE us.id = ?1")
	boolean isBanned(Integer userId);
}
