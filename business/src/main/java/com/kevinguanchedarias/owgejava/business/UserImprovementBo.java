package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.UserImprovement;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.UserImprovementRepository;

@Service
public class UserImprovementBo implements Serializable {
	private static final long serialVersionUID = -3549407827748093566L;

	@Autowired
	private UserImprovementRepository userImprovementRepository;

	/**
	 * Will return the user's improvements<br />
	 * <b>NOTICE:</b> Never returns null, new instance if null
	 * 
	 * @param user
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public UserImprovement findUserImprovements(UserStorage user) {
		return findOrNew(user);
	}

	/**
	 * Adds the improvements to the user and <b>saves</b> to database
	 * 
	 * @param improvement
	 *            improvement to add
	 * @param user
	 * @author Kevin Guanche Darias
	 */
	public void addImprovements(Improvement improvement, UserStorage user) {
		addImprovements(improvement, user, 1L);
	}

	/**
	 * Adds the improvements to the user and <b>saves</b> to database
	 * 
	 * @param improvement
	 *            improvement to add
	 * @param user
	 * @param count
	 *            Number of times to add it
	 * @author Kevin Guanche Darias
	 */
	public void addImprovements(Improvement improvement, UserStorage user, Long count) {
		iterateImprovementsAndSave(improvement, user, count, true);
	}

	/**
	 * Subtracts the improvements to the user and <b>saves</b> to database
	 * 
	 * @param improvement
	 * @param user
	 * @author Kevin Guanche Darias
	 */
	public void subtractImprovements(Improvement improvement, UserStorage user) {
		UserImprovement currentImprovements = findOrNew(user);
		operateImprovements(currentImprovements, improvement, false);
		userImprovementRepository.save(currentImprovements);
	}

	public void subtractImprovements(Improvement improvement, UserStorage user, Long count) {
		iterateImprovementsAndSave(improvement, user, count, false);
	}

	/**
	 * Finds the UserImprovements for given user Notice: <b>If there is no value
	 * will return a new <b>persisted instance</b>
	 * 
	 * @param user
	 * @return The new <b>persitent</b> instance with the user object already
	 *         assigned
	 * @author Kevin Guanche Darias
	 */
	private UserImprovement findOrNew(UserStorage user) {
		UserImprovement retVal = userImprovementRepository.findOneByUserId(user.getId());
		if (retVal == null) {
			retVal = new UserImprovement();
			retVal.setUser(user);
			userImprovementRepository.save(retVal);
		}
		return retVal;
	}

	/**
	 * Adds or subtracts source to userImprovement
	 * 
	 * @param userImprovement
	 * @param source
	 * @param sum
	 *            If true will add, if false will subtract
	 * @author Kevin Guanche Darias
	 */
	private void operateImprovements(UserImprovement userImprovement, Improvement source, boolean sum) {
		int sign;
		if (sum) {
			sign = 1;
		} else {
			sign = -1;
		}

		userImprovement
				.addMorePrimaryResourceProduction(
						ObjectUtils.firstNonNull(source.getMorePrimaryResourceProduction(), 0F) * sign)
				.addMoreSecondaryResourceProduction(
						ObjectUtils.firstNonNull(source.getMoreSecondaryResourceProduction(), 0F) * sign)
				.addMoreEnergyProduction(ObjectUtils.firstNonNull(source.getMoreEnergyProduction(), 0F) * sign)
				.addMoreChargeCapacity(ObjectUtils.firstNonNull(source.getMoreChargeCapacity(), 0F) * sign)
				.addMoreMissions(ObjectUtils.firstNonNull(source.getMoreMisions(), 0F) * sign)
				.addMoreUpgradeResearchSpeed(ObjectUtils.firstNonNull(source.getMoreUpgradeResearchSpeed(), 0F) * sign)
				.addMoreUnitBuildSpeed(ObjectUtils.firstNonNull(source.getMoreUnitBuildSpeed(), 0F) * sign);
	}

	private void iterateImprovementsAndSave(Improvement improvement, UserStorage user, Long count, boolean sum) {
		UserImprovement currentImprovements = findOrNew(user);
		for (long i = 0; i < count; i++) {
			operateImprovements(currentImprovements, improvement, sum);
		}
		userImprovementRepository.save(currentImprovements);
	}
}
