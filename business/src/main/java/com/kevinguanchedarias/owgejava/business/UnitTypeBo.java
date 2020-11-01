package com.kevinguanchedarias.owgejava.business;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;

@Component
public class UnitTypeBo implements WithNameBo<Integer, UnitType, UnitTypeDto> {
	private static final long serialVersionUID = 1064115662505668879L;

	private static final String UNIT_TYPE_CHANGE = "unit_type_change";

	@Autowired
	private UnitTypeRepository unitTypeRepository;

	@Autowired
	private ImprovementBo improvementBo;

	@Autowired
	@Lazy
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	@Lazy
	private UnitMissionBo unitMissionBo;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private SocketIoService socketIoService;

	@Override
	public JpaRepository<UnitType, Integer> getRepository() {
		return unitTypeRepository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<UnitTypeDto> getDtoClass() {
		return UnitTypeDto.class;
	}

	/**
	 * Finds the max amount of a certain unit type the given user can have <br>
	 * <b>NOTICE: It will proccess all the obtained upgrades and obtained units to
	 * find the improvement</b>
	 *
	 * @param user
	 * @param typeId
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long findUniTypeLimitByUser(UserStorage user, Integer typeId) {
		UnitType type = findById(typeId);
		return findUniTypeLimitByUser(user, type);
	}

	/**
	 * Finds the max amount of a certain unit type the given user can have <br>
	 * <b>NOTICE: It will proccess all the obtained upgrades and obtained units to
	 * find the improvement</b>
	 *
	 * @param user
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public Long findUniTypeLimitByUser(UserStorage user, UnitType type) {
		Long retVal = 0L;
		if (type.hasMaxCount()) {
			GroupedImprovement groupedImprovement = improvementBo.findUserImprovement(user);
			retVal = (long) Math.floor(improvementBo.computeImprovementValue(type.getMaxCount(),
					groupedImprovement.findUnitTypeImprovement(ImprovementTypeEnum.AMOUNT, type)));
		}
		return retVal;
	}

	/**
	 * Test if the specified unit type can run the specified mission
	 *
	 * @param user         user that is going to run the mission (used to test
	 *                     planet ownership... if required)
	 * @param targetPlanet Target mission planet (used to test planet ownership...
	 *                     if required)
	 * @param unitType     Unit type to test
	 * @param type         Mission to execute
	 * @return
	 * @deprecated Please Use
	 *             {@link UnitMissionBo#canDoMission(UserStorage, Planet, com.kevinguanchedarias.owgejava.entity.EntityWithMissionLimitation, MissionType)}
	 * @throws SgtCorruptDatabaseException     Value in unit types database table is
	 *                                         not an accepted value
	 * @throws SgtBackendInvalidInputException Mission is not supported
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.9.0")
	public boolean canDoMission(UserStorage user, Planet targetPlanet, UnitType unitType, MissionType type) {
		return unitMissionBo.canDoMission(user, targetPlanet, unitType, type);
	}

	public boolean canDoMission(UserStorage user, Planet targetPlanet, List<UnitType> unitTypes, MissionType type) {
		return unitTypes.stream().allMatch(current -> canDoMission(user, targetPlanet, current, type));
	}

	/**
	 *
	 * @param userId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<UnitTypeDto> findUnitTypesWithUserInfo(Integer userId) {
		return findUnitTypesWithUserInfo(userStorageBo.findById(userId));
	}

	/**
	 *
	 * @param user
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<UnitTypeDto> findUnitTypesWithUserInfo(UserStorage user) {
		return findAll().stream().map(current -> {
			UnitTypeDto currentDto = new UnitTypeDto();
			currentDto.dtoFromEntity(current);
			currentDto.setComputedMaxCount(findUniTypeLimitByUser(user, current));
			if (current.hasMaxCount()) {
				currentDto.setUserBuilt(obtainedUnitBo.countByUserAndUnitType(user, current));
			}
			return currentDto;
		}).collect(Collectors.toList());
	}

	/**
	 *
	 * @param user
	 * @since 0.9.7
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void emitUserChange(UserStorage user) {
		emitUserChange(user.getId());
	}

	public void emitUserChange(Integer userId) {
		socketIoService.sendMessage(userId, UNIT_TYPE_CHANGE, () -> findUnitTypesWithUserInfo(userId));
	}
}
