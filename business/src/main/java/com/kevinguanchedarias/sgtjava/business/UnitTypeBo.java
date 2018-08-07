package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.ImprovementType;
import com.kevinguanchedarias.sgtjava.repository.UnitTypeRepository;

@Component
public class UnitTypeBo implements WithNameBo<UnitType> {
	private static final long serialVersionUID = 1064115662505668879L;

	@Autowired
	private UnitTypeRepository unitTypeRepository;

	@Autowired
	private ImprovementBo improvementBo;

	@Override
	public JpaRepository<UnitType, Number> getRepository() {
		return unitTypeRepository;
	}

	/**
	 * Finds the max amount of a certain unit type the given user can have <br>
	 * <b>NOTICE: It will proccess all the obtained upgrades and obtained units
	 * to find the improvement</b>
	 * 
	 * @param user
	 * @param typeId
	 * @return
	 * @todo In the future, should cache this value when possible
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long findUniTypeLimitByUser(UserStorage user, Integer typeId) {
		UnitType type = findById(typeId);
		Long retVal = 0L;
		if (type.hasMaxCount()) {
			retVal = improvementBo
					.computeImprovementValue(type.getMaxCount(),
							improvementBo.sumUnitTypeImprovementByUserAndImprovementType(user, ImprovementType.AMOUNT))
					.longValue();
		}
		return retVal;
	}
}
