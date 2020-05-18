package com.kevinguanchedarias.owgejava.business;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.interfaces.ImprovementSource;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;

@Service
public class ObtainedUnitBo implements BaseBo<Long, ObtainedUnit, ObtainedUnitDto>, ImprovementSource {
	private static final long serialVersionUID = -2056602917496640872L;

	@Autowired
	private ObtainedUnitRepository repository;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private PlanetBo planetBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	@Autowired
	private UnitMissionBo unitMissionBo;

	@Autowired
	private ImprovementBo improvementBo;

	@Override
	public JpaRepository<ObtainedUnit, Long> getRepository() {
		return repository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<ObtainedUnitDto> getDtoClass() {
		return ObtainedUnitDto.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.interfaces.ImprovementSource#
	 * calculateImprovement(com.kevinguanchedarias.owgejava.entity.UserStorage)
	 */
	@Override
	public GroupedImprovement calculateImprovement(UserStorage user) {
		GroupedImprovement groupedImprovement = new GroupedImprovement();
		findNotBuilding(user.getId()).forEach(current -> groupedImprovement.add(current.getUnit().getImprovement()));
		return groupedImprovement;
	}

	@PostConstruct
	public void init() {
		improvementBo.addImprovementSource(this);
	}

	/**
	 * Finds all the user obtained units that are <b>not</b> in a building state
	 * 
	 * @param userId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> findNotBuilding(Integer userId) {
		return repository.findByUserAndNotBuilding(userId);
	}

	public boolean hasUnitsInPlanet(UserStorage user, Planet planet) {
		return hasUnitsInPlanet(user.getId(), planet.getId());
	}

	/**
	 * Note: Takes into account also the units involved in missions that originate
	 * from this planet, even if they are outside now
	 * 
	 * @param userId
	 * @param planetId
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean hasUnitsInPlanet(Integer userId, Long planetId) {
		return repository.countByUserIdAndSourcePlanetId(userId, planetId) > 0;
	}

	public List<ObtainedUnit> findByMissionId(Long missionId) {
		return repository.findByMissionId(missionId);
	}

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetId(Integer userId, Integer unitId, Long sourcePlanetId) {
		return repository.findOneByUserIdAndUnitIdAndSourcePlanetId(userId, unitId, sourcePlanetId);
	}

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndIddNoAndMissionNull(Integer userId, Integer unitId,
			Long sourcePlanetId, Long id) {
		return repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndIdNotAndMissionNull(userId, unitId,
				sourcePlanetId, id);
	}

	public List<ObtainedUnit> findByUserIdAndSourcePlanetAndMissionIdIsNull(UserStorage user, Planet targetPlanet) {
		return findByUserIdAndSourcePlanetAndMissionIdIsNull(user.getId(), targetPlanet.getId());
	}

	public List<ObtainedUnit> findByUserIdAndSourcePlanetAndMissionIdIsNull(Integer userId, Long planetId) {
		return repository.findByUserIdAndSourcePlanetIdAndMissionIdIsNull(userId, planetId);
	}

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNullOrDeployed(Integer userId,
			Integer unitId, Long planetId) {
		return repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNullOrDeployed(userId, unitId, planetId);
	}

	/**
	 * 
	 * @since 0.8.1
	 * @param userId
	 * @param unitId
	 * @param planetId
	 * @return
	 */
	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(Integer userId, Integer unitId,
			Long planetId) {
		return repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIsNull(userId, unitId, planetId);
	}

	public ObtainedUnit findOneByUserIdAndUnitId(Integer userId, Integer unitId) {
		return repository.findOneByUserIdAndUnitId(userId, unitId);
	}

	/**
	 * 
	 * @param userId
	 * @param id
	 * @param id2
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public ObtainedUnit findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(Integer userId, Integer unitId,
			Long planetId) {
		return repository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndMissionTypeCode(userId, unitId, planetId,
				MissionType.DEPLOYED.name());
	}

	public Long countByUserAndUnitId(UserStorage user, Integer unitId) {
		return repository.countByUserIdAndUnitId(user.getId(), unitId);
	}

	public Long countByUserAndUnitType(UserStorage user, Integer typeId) {
		UnitType type = unitTypeBo.findById(typeId);
		return countByUserAndUnitType(user, type);
	}

	/**
	 * 
	 * @param user
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public Long countByUserAndUnitType(UserStorage user, UnitType type) {
		return repository.countByUserAndUnitType(user, type);
	}

	/**
	 * Returns the units in the <i>targetPlanet</i> that are not in mission <br>
	 * Ideally used to explore a planet
	 * 
	 * @param exploreMission mission that is executing the explore
	 * @param targetPlanet
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> explorePlanetUnits(Mission exploreMission, Planet targetPlanet) {
		return repository.findByExplorePlanet(exploreMission.getId(), targetPlanet.getId());
	}

	/**
	 * Saves the obtained unit to the database <br>
	 * <b>IMPORTANT:</b> it may change the id, use the resultant value <br>
	 * Will add the count to existing one, <b>if it exists in the planet</b>
	 * 
	 * @param userId
	 * @param obtainedUnit <b>NOTICE:</b> Won't be changed from inside
	 * @param targetPlanet Planet to where you are adding the units
	 * @return new instance of saved obtained unit
	 * @author Kevin Guanche Darias
	 */
	public ObtainedUnit saveWithAdding(Integer userId, ObtainedUnit obtainedUnit, Long targetPlanet) {
		Integer unitId = obtainedUnit.getUnit().getId();
		ObtainedUnit retVal;
		ObtainedUnit existingOne = planetBo.isOfUserProperty(userId, targetPlanet)
				? findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(userId, unitId, targetPlanet)
				: findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(userId, unitId, targetPlanet);
		if (existingOne == null) {
			retVal = save(obtainedUnit);
		} else {
			existingOne.setCount(existingOne.getCount() + obtainedUnit.getCount());
			retVal = save(existingOne);
			if (obtainedUnit.getId() != null) {
				delete(obtainedUnit);
			}
		}
		return retVal;
	}

	@Transactional
	public ObtainedUnitDto saveWithSubtraction(ObtainedUnitDto obtainedUnitDto, boolean handleImprovements) {
		ObtainedUnitDto retVal;
		ObtainedUnit obtainedUnit = saveWithSubtraction(findByIdOrDie(obtainedUnitDto.getId()),
				obtainedUnitDto.getCount(), handleImprovements);
		if (obtainedUnit != null) {
			retVal = new ObtainedUnitDto();
			retVal.dtoFromEntity(obtainedUnit);
		} else {
			retVal = null;
		}
		return retVal;
	}

	/**
	 * Saves the Obtained unit with subtraction
	 * 
	 * @param obtainedUnit       Target obtained unit
	 * @param substractionCount  Count to subtract
	 * @param handleImprovements If specified will sustract the improvements too
	 * @return saved obtained unit, null if the count is the same
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public ObtainedUnit saveWithSubtraction(ObtainedUnit obtainedUnit, Long substractionCount,
			boolean handleImprovements) {
		if (handleImprovements) {
			improvementBo.clearSourceCache(obtainedUnit.getUser(), this);
		}
		if (substractionCount > obtainedUnit.getCount()) {
			throw new SgtBackendInvalidInputException(
					"Can't not subtract because, obtainedUnit count is less than the amount to subtract");
		} else if (obtainedUnit.getCount() > substractionCount) {
			obtainedUnit.setCount(obtainedUnit.getCount() - substractionCount);
			return save(obtainedUnit);
		} else if (obtainedUnit.getCount().equals(substractionCount)) {
			delete(obtainedUnit);
			return null;
		} else {
			throw new ProgrammingException("Should never ever happend");
		}
	}

	/**
	 * Searches an ObtainedUnit in <i>storage</i> having an unit with the <b>same
	 * id</b> than <i>searchValue</i>
	 * 
	 * @param storage     List that will be searched through
	 * @param searchValue Value that is going to be search inside <i>storage</i>
	 * @return ObtainedUnit found or <b>null if NOT found</b>
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObtainedUnit findHavingSameUnit(List<ObtainedUnit> storage, ObtainedUnit searchValue) {
		return storage.stream()
				.filter(currentUnit -> searchValue.getUnit().getId().equals(currentUnit.getUnit().getId())).findFirst()
				.orElse(null);
	}

	/**
	 * Deletes obtained units involved in passed mission <br>
	 * <b>NOTICE: </b> By default will subtract improvements
	 * 
	 * @param missionId
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public int deleteByMissionId(Long missionId) {
		int retVal = repository.deleteByMissionId(missionId).intValue();
		improvementBo.clearCacheEntries(this);
		return retVal;
	}

	/**
	 * Deletes obtained units involved in passed mission <br>
	 * <b>NOTICE: </b> As of 0.7.3, the param <i>subtractImprovements</i> can be
	 * specified, for example to avoid removing improvements of a unit that has not
	 * been even built
	 * 
	 * @deprecated Use the version without the <i>subtractImprovements</i> param, as
	 *             it's not longer required
	 * @param missionId
	 * @param subtractImprovements If true will too subtract improvements
	 * @return
	 * @since 0.7.3
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.1")
	@Transactional(propagation = Propagation.MANDATORY)
	public int deleteByMissionId(Long missionId, boolean subtractImprovements) {
		int retVal = repository.findByMissionId(missionId).stream().map(current -> {
			delete(current);
			return current;
		}).collect(Collectors.toList()).size();
		if (subtractImprovements) {
			improvementBo.clearCacheEntries(this);

		}
		return retVal;
	}

	public List<ObtainedUnit> findInMyPlanet(Long planetId) {
		userStorageBo.checkOwnPlanet(planetId);
		return repository.findBySourcePlanetIdAndMissionNull(planetId);
	}

	/**
	 * Finds the involved units in an attack
	 * 
	 * @param attackedPlanet
	 * @param attackMission
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> findInvolvedInAttack(Planet attackedPlanet) {
		List<ObtainedUnit> retVal = new ArrayList<>();
		retVal.addAll(repository.findBySourcePlanetIdAndMissionIsNull(attackedPlanet.getId()));
		retVal.addAll(repository.findByTargetPlanetId(attackedPlanet.getId()));
		return retVal;
	}

	public Long deleteBySourcePlanetIdAndMissionIdNull(Planet sourcePlanet) {
		return repository.deleteBySourcePlanetIdAndMissionIdNull(sourcePlanet.getId());
	}

	public boolean existsByMission(Mission mission) {
		return repository.countByMission(mission) > 0;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void moveUnit(ObtainedUnit unit, Integer userId, Long planetId) {
		Planet planet = planetBo.findById(planetId);
		unit.setSourcePlanet(unit.getSourcePlanet());
		unit.setTargetPlanet(planet);
		if (planetBo.isOfUserProperty(userId, planetId)) {
			unit.setSourcePlanet(planet);
			saveWithAdding(userId, unit, planetId);
			unit.setMission(null);
			unit.setTargetPlanet(null);
			unit.setFirstDeploymentMission(null);
		} else if (MissionType.valueOf(unit.getMission().getType().getCode()) == MissionType.DEPLOYED) {
			save(unit);
		} else {
			unit = saveWithAdding(userId, unit, planetId);
			if (MissionType.valueOf(unit.getMission().getType().getCode()) != MissionType.DEPLOYED) {
				unit.setMission(unitMissionBo.findDeployedMissionOrCreate(unit));
				save(unit);
			}
		}
	}

	public Double findConsumeEnergyByUser(UserStorage user) {
		return ObjectUtils.firstNonNull(repository.computeConsumedEnergyByUser(user), 0D);
	}

	/**
	 * Returns the total sum of the value for the specified improvement type for
	 * user obtained unit
	 * 
	 * @param user
	 * @param type The expected type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long sumUnitTypeImprovementByUserAndImprovementType(UserStorage user, ImprovementTypeEnum type) {
		return ObjectUtils.firstNonNull(repository.sumByUserAndImprovementUnitTypeImprovementType(user, type.name()),
				0L);
	}

	public boolean hasReachedUnitTypeLimit(UserStorage user, Integer typeId) {
		UnitType type = unitTypeBo.findById(typeId);
		return type.hasMaxCount()
				&& repository.countByUserAndUnitType(user, type) >= unitTypeBo.findUniTypeLimitByUser(user, typeId);
	}

	/**
	 * 
	 * @param user
	 * @param typeId
	 * @param count  Count to test if would exceed the unit type limit
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean wouldReachUnitTypeLimit(UserStorage user, Integer typeId, Long count) {
		UnitType type = unitTypeBo.findById(typeId);
		Long userCount = ObjectUtils.firstNonNull(repository.countByUserAndUnitType(user, type), 0L) + count;
		return type.hasMaxCount() && userCount > unitTypeBo.findUniTypeLimitByUser(user, typeId);
	}

	/**
	 * Checks if the specified count would be over the expected count
	 * 
	 * @param user
	 * @param typeId
	 * @param count
	 * @throws SgtBackendInvalidInputException When reached
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void checkWouldReachUnitTypeLimit(UserStorage user, Integer typeId, Long count) {
		if (wouldReachUnitTypeLimit(user, typeId, count)) {
			throw new SgtBackendInvalidInputException(
					"Nice try to buy over your possibilities!!!, try outside of Spain!");
		}
	}

	/**
	 * Gets the ENUM value of the unit mission type (or null )
	 * 
	 * @param unit
	 * @return
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MissionType resolveMissionType(ObtainedUnit unit) {
		if (unit.getMission() != null) {
			return MissionType.valueOf(unit.getMission().getType().getCode());
		} else {
			return null;
		}
	}

	public Mission findPlanetDeployedMission(Integer userId, Planet planet) {
		return unitMissionBo.findOneByUserIdAndTypeAndTargetPlanet(userId, MissionType.DEPLOYED, planet.getId());
	}

}
