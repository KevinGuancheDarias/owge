package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.ObtainedUpgrade;

public interface ObtainedUpgradeRepository extends JpaRepository<ObtainedUpgrade, Number>, Serializable {
	public ObtainedUpgrade findOneByUserIdIdAndUpgradeId(Integer userId, Integer upgradeId);

	public List<ObtainedUpgrade> findByUserIdId(Integer userId);
}
