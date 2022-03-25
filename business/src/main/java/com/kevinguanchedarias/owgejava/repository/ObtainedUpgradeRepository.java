package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;
import java.util.List;

public interface ObtainedUpgradeRepository extends JpaRepository<ObtainedUpgrade, Long>, Serializable {

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    void deleteByUpgrade(Upgrade upgrade);

    boolean existsByUserIdAndUpgradeId(Integer userId, Integer upgradeId);
    
    ObtainedUpgrade findOneByUserIdAndUpgradeId(Integer userId, Integer upgradeId);

    List<ObtainedUpgrade> findByUserId(Integer userId);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    List<ObtainedUpgrade> findByUpgrade(Upgrade upgrade);

    @Query("SELECT SUM(utg.value * ou.level) FROM ObtainedUpgrade ou INNER JOIN ou.upgrade.improvement.unitTypesUpgrades utg WHERE ou.user.id = ?1 AND utg.type = ?2")
    Long sumByUserAndImprovementUnitTypeImprovementType(UserStorage user, String improvementType);

}
