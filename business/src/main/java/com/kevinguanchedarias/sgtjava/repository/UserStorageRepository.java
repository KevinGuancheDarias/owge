package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.UserStorage;

public interface UserStorageRepository extends JpaRepository<UserStorage, Number>, Serializable {
	public UserStorage findOneByIdAndFactionId(Integer userId, Integer factionId);
}
