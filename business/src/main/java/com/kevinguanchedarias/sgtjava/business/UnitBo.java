package com.kevinguanchedarias.sgtjava.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.sgtjava.repository.UnitRepository;

@Component
public class UnitBo implements WithNameBo<Unit> {
	private static final long serialVersionUID = 8956360591688432113L;

	@Autowired
	private UnitRepository unitRepository;

	@Autowired
	private UnlockedRelationBo unlockedRelationBo;

	@Override
	public JpaRepository<Unit, Number> getRepository() {
		return unitRepository;
	}

	public List<Unit> findUnlocked(UserStorage user) {
		return unlockedRelationBo.unboxToTargetEntity(
				unlockedRelationBo.findByUserIdAndObjectType(user.getId(), RequirementTargetObject.UNIT));
	}

	/**
	 * Calculates the requirements according to the count to operate!
	 * 
	 * @param unitId
	 * @param count
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public ResourceRequirementsPojo calculateRequirements(Integer unitId, Long count) {
		return calculateRequirements(findByIdOrDie(unitId), count);
	}

	/**
	 * Calculates the requirements according to the count to operate!
	 * 
	 * @param unit
	 * @param count
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public ResourceRequirementsPojo calculateRequirements(Unit unit, Long count) {
		if (count < 1) {
			throw new SgtBackendInvalidInputException("Input can't be negative");
		}

		ResourceRequirementsPojo retVal = new ResourceRequirementsPojo();
		retVal.setRequiredPrimary((double) (unit.getPrimaryResource() * count));
		retVal.setRequiredSecondary((double) (unit.getSecondaryResource() * count));
		retVal.setRequiredTime((double) (unit.getTime() * count));
		return retVal;
	}
}
