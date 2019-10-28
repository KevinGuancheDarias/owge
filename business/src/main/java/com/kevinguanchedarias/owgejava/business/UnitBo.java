package com.kevinguanchedarias.owgejava.business;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;

@Component
public class UnitBo implements WithNameBo<Integer, Unit, UnitDto> {
	private static final long serialVersionUID = 8956360591688432113L;

	@Autowired
	private UnitRepository unitRepository;

	@Autowired
	private UnlockedRelationBo unlockedRelationBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Override
	public JpaRepository<Unit, Integer> getRepository() {
		return unitRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<UnitDto> getDtoClass() {
		return UnitDto.class;
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
	 * @throws SgtBackendInvalidInputException can't be negative
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
		retVal.setRequiredEnergy((double) (ObjectUtils.firstNonNull(unit.getEnergy(), 0) * count));
		return retVal;
	}

	public boolean isUnique(Unit unit) {
		return unit.getIsUnique();
	}

	/**
	 * Checks if the unique unit has been build by the user
	 * 
	 * @param user
	 * @param unit
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void checkIsUniqueBuilt(UserStorage user, Unit unit) {
		if (isUnique(unit) && obtainedUnitBo.countByUserAndUnitId(user, unit.getId()) > 0) {
			throw new SgtBackendInvalidInputException(
					"Unit with id " + unit.getId() + " has been already build by user " + user.getId());
		}

	}

}
