package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface UserStorageRepository extends JpaRepository<UserStorage, Number>, Serializable {
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
}
