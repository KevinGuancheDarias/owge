package com.kevinguanchedarias.sgtjava.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.sgtjava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.sgtjava.repository.ObtainedUpgradeRepository;

@Component
public class ObtainedUpradeBo implements BaseBo<ObtainedUpgrade> {
	private static final long serialVersionUID = 2294363946431892708L;

	@Autowired
	private ObtainedUpgradeRepository obtainedUpgradeRepository;

	@Override
	public JpaRepository<ObtainedUpgrade, Number> getRepository() {
		return obtainedUpgradeRepository;
	}

	/**
	 * Returns obtained upgrades by given user
	 * 
	 * @param userId
	 *            id of the user
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<ObtainedUpgrade> findByUser(Integer userId) {
		return obtainedUpgradeRepository.findByUserIdId(userId);
	}

	public ObtainedUpgrade findByUserAndUpgrade(Integer userId, Integer upgradeId) {
		return obtainedUpgradeRepository.findOneByUserIdIdAndUpgradeId(userId, upgradeId);
	}

	/**
	 * Does user has the given upgrade obtained?
	 * 
	 * @param userId
	 *            id of the user
	 * @param upgradeId
	 *            id of the asked upgrade
	 * @return true if upgrade has been obtained
	 * @author Kevin Guanche Darias
	 */
	public boolean userHasUpgrade(Integer userId, Integer upgradeId) {
		return findUserObtainedUpgrade(userId, upgradeId) != null;
	}

	/**
	 * Find user's obtained upgrade
	 * 
	 * @param userId
	 * @param upgradeId
	 * @author Kevin Guanche Darias
	 */
	public ObtainedUpgrade findUserObtainedUpgrade(Integer userId, Integer upgradeId) {
		return obtainedUpgradeRepository.findOneByUserIdIdAndUpgradeId(userId, upgradeId);
	}
}
