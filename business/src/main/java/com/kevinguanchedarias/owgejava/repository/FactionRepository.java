package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import com.kevinguanchedarias.owgejava.entity.Faction;

public interface FactionRepository extends WithNameRepository<Faction, Integer>, Serializable {
	public List<Faction> findByHiddenFalse();

	public Long countByHiddenFalseAndId(Integer id);
}
