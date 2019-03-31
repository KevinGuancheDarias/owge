package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface ObtainedUpgradeRepository extends JpaRepository<ObtainedUpgrade, Number>, Serializable {
	public ObtainedUpgrade findOneByUserIdIdAndUpgradeId(Integer userId, Integer upgradeId);

	public List<ObtainedUpgrade> findByUserIdId(Integer userId);

	@Query("SELECT SUM(utg.value * ou.level) FROM ObtainedUpgrade ou INNER JOIN ou.upgrade.improvement.unitTypesUpgrades utg WHERE ou.userId = ?1 AND utg.type = ?2")
	public Long sumByUserAndImprovementUnitTypeImprovementType(UserStorage user, String improvementType);
}
