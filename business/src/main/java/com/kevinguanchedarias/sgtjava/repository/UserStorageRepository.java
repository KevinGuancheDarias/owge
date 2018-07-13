package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;

public interface UserStorageRepository extends JpaRepository<UserStorage, Number>, Serializable {
	public UserStorage findOneByIdAndFactionId(Integer userId, Integer factionId);

	@Modifying
	@Query("UPDATE UserStorage u SET u.points = u.points + :points WHERE u = :user")
	public void addPointsToUser(@Param("user") UserStorage user, @Param("points") Double points);

	public UserStorage findOneByMissions(Mission mission);
}
