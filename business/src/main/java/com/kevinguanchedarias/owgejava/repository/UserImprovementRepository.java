package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.UserImprovement;

/**
 * 
 * @deprecated No longer store the current values in the database
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Deprecated(since = "0.8.0")
public interface UserImprovementRepository extends JpaRepository<UserImprovement, Number>, Serializable {
	public UserImprovement findOneByUserId(Integer userId);
}
