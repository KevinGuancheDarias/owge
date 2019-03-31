package com.kevinguanchedarias.owgejava.pojo;

import java.util.HashMap;
import java.util.Map;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementType;

/**
 * Represents the sum of all the unit type improvements for given user
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class UserUnitTypeImprovement {

	private Map<String, Long> improvements = new HashMap<>();

	public UserUnitTypeImprovement(UserStorage user, ImprovementBo improvementBo) {
		loadValues(user, improvementBo);
	}

	public Long findValue(ImprovementType type) {
		return improvements.get(type.name());
	}

	public Float findValueRational(ImprovementType type) {
		return findValue(type) / 100F;
	}

	private void loadValues(UserStorage user, ImprovementBo improvementBo) {
		addImprovementType(user, improvementBo, ImprovementType.ATTACK)
				.addImprovementType(user, improvementBo, ImprovementType.DEFENSE)
				.addImprovementType(user, improvementBo, ImprovementType.SHIELD);
	}

	private UserUnitTypeImprovement addImprovementType(UserStorage user, ImprovementBo improvementBo,
			ImprovementType type) {
		improvements.put(type.name(), improvementBo.sumUnitTypeImprovementByUserAndImprovementType(user, type));
		return this;
	}
}
