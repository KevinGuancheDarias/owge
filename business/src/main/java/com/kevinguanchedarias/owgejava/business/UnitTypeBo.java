package com.kevinguanchedarias.owgejava.business;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementType;
import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;

@Component
public class UnitTypeBo implements WithNameBo<UnitType> {
	private static final long serialVersionUID = 1064115662505668879L;

	@Autowired
	private UnitTypeRepository unitTypeRepository;

	@Autowired
	private ImprovementBo improvementBo;

	@Autowired
	private PlanetBo planetBo;

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

	/**
	 * Test if the specified unit type can run the specified mission
	 * 
	 * @param user
	 *            user that is going to run the mission (used to test planet
	 *            ownership... if required)
	 * @param targetPlanet
	 *            Target mission planet (used to test planet ownership... if
	 *            required)
	 * @param unitType
	 *            Unit type to test
	 * @param type
	 *            Mission to execute
	 * @return
	 * @throws SgtCorruptDatabaseException
	 *             Value in unit types database table is not an accepted value
	 * @throws SgtBackendInvalidInputException
	 *             Mission is not supported
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean canDoMission(UserStorage user, Planet targetPlanet, UnitType unitType, MissionType type) {
		String targetMethod = "getCan" + WordUtils.capitalizeFully(type.name(), '_').replaceAll("_", "");
		try {
			MissionSupportEnum missionSupport = ((MissionSupportEnum) unitType.getClass().getMethod(targetMethod)
					.invoke(unitType));
			switch (missionSupport) {
			case ANY:
				return true;
			case OWNED_ONLY:
				return planetBo.isOfUserProperty(user, targetPlanet);
			case NONE:
				return false;
			default:
				throw new SgtCorruptDatabaseException(
						"unsupported mission support was specified: " + missionSupport.name());
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new SgtBackendInvalidInputException(
					"Could not invoke method " + targetMethod + " maybe it is not supported mission", e);
		}
	}

	public boolean canDoMission(UserStorage user, Planet targetPlanet, List<UnitType> unitTypes, MissionType type) {
		return unitTypes.stream().allMatch(current -> canDoMission(user, targetPlanet, current, type));
	}
}
