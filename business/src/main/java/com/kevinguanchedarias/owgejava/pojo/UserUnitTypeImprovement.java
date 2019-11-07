package com.kevinguanchedarias.owgejava.pojo;

import java.util.HashMap;
import java.util.Map;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.ImprovementUnitTypeBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;

/**
 * Represents the sum of all the unit type improvements for given user
 *
 * @deprecated Use {@link GroupedImprovement} instead, obtained from
 *             {@link ImprovementBo#findUserImprovement(UserStorage)}
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Deprecated(since = "0.8.0")
public class UserUnitTypeImprovement {

	private Map<String, Long> improvements = new HashMap<>();

	public UserUnitTypeImprovement(UserStorage user, ImprovementUnitTypeBo improvementBo) {
		loadValues(user, improvementBo);
	}

	public Long findValue(ImprovementTypeEnum type) {
		return improvements.get(type.name());
	}

	public Float findValueRational(ImprovementTypeEnum type) {
		return findValue(type) / 100F;
	}

	private void loadValues(UserStorage user, ImprovementUnitTypeBo improvementBo) {
		addImprovementType(user, improvementBo, ImprovementTypeEnum.ATTACK)
				.addImprovementType(user, improvementBo, ImprovementTypeEnum.DEFENSE)
				.addImprovementType(user, improvementBo, ImprovementTypeEnum.SHIELD);
	}

	private UserUnitTypeImprovement addImprovementType(UserStorage user, ImprovementUnitTypeBo improvementBo,
			ImprovementTypeEnum type) {
		improvements.put(type.name(), improvementBo.sumUnitTypeImprovementByUserAndImprovementType(user, type));
		return this;
	}
}
