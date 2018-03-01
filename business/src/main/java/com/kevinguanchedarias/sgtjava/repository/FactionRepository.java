package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import com.kevinguanchedarias.sgtjava.entity.Faction;

public interface FactionRepository extends WithNameRepository<Faction, Number>, Serializable {
	public List<Faction> findByHiddenFalse();

	public Long countByHiddenFalseAndId(Integer id);
}
