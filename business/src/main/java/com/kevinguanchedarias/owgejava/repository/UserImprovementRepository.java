package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.UserImprovement;

public interface UserImprovementRepository extends JpaRepository<UserImprovement, Number>, Serializable {
	public UserImprovement findOneByUserId(Integer userId);
}
