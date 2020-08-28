package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface ObtainedUpgradeRepository extends JpaRepository<ObtainedUpgrade, Long>, Serializable {

	/**
	 *
	 * @param upgrade
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void deleteByUpgrade(Upgrade upgrade);

	public ObtainedUpgrade findOneByUserIdIdAndUpgradeId(Integer userId, Integer upgradeId);

	public List<ObtainedUpgrade> findByUserIdId(Integer userId);

	/**
	 *
	 * @param upgrade
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUpgrade> findByUpgrade(Upgrade upgrade);

	@Query("SELECT SUM(utg.value * ou.level) FROM ObtainedUpgrade ou INNER JOIN ou.upgrade.improvement.unitTypesUpgrades utg WHERE ou.userId = ?1 AND utg.type = ?2")
	public Long sumByUserAndImprovementUnitTypeImprovementType(UserStorage user, String improvementType);

}
