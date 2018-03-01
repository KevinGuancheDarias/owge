package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.Universe;

public interface UniverseRepository extends JpaRepository<Universe, Number>, Serializable {
	public List<Universe> findByOfficial(Boolean isOfficial);

	public List<Universe> findByCreatorId(Integer userId);
}
