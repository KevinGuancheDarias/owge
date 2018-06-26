package com.kevinguanchedarias.sgtjava.business;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.exception.ProgrammingException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.sgtjava.repository.ObtainedUnitRepository;

@Service
public class ObtainedUnitBo implements BaseBo<ObtainedUnit> {
	private static final long serialVersionUID = -2056602917496640872L;

	@Autowired
	private ObtainedUnitRepository repository;

	@Autowired
	private UserStorageBo userStorageBo;

	@Override
	public JpaRepository<ObtainedUnit, Number> getRepository() {
		return repository;
	}

	public List<ObtainedUnit> findByMissionId(Long missionId) {
		return repository.findByMissionId(missionId);
	}

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetId(Integer userId, Integer unitId, Long sourcePlanetId) {
		return repository.findOneByUserIdAndUnitIdAndSourcePlanetId(userId, unitId, sourcePlanetId);
	}

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndItNot(Integer userId, Integer unitId,
			Long sourcePlanetId, Long id) {
		return repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndIdNot(userId, unitId, sourcePlanetId, id);
	}

	public List<ObtainedUnit> findByUserIdAndSourcePlanetAndMissionIdIsNull(UserStorage user, Planet targetPlanet) {
		return findByUserIdAndSourcePlanetAndMissionIdIsNull(user.getId(), targetPlanet.getId());
	}

	public List<ObtainedUnit> findByUserIdAndSourcePlanetAndMissionIdIsNull(Integer userId, Long planetId) {
		return repository.findByUserIdAndSourcePlanetIdAndMissionIdIsNull(userId, planetId);
	}

	public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIdIsNull(Integer userId, Integer unitId,
			Long planetId) {
		return repository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndMissionIdIsNull(userId, unitId, planetId);
	}

	public ObtainedUnit findOneByUserIdAndUnitId(Integer userId, Integer unitId) {
		return repository.findOneByUserIdAndUnitId(userId, unitId);
	}

	public Long countByUserAndUnitId(UserStorage user, Integer unitId) {
		return repository.countByUserIdAndUnitId(user.getId(), unitId);
	}

	/**
	 * Returns the units in the <i>targetPlanet</i> that are not in mission <br>
	 * Ideally used to explore a planet
	 * 
	 * @param targetPlanet
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> explorePlanetUnits(Planet targetPlanet) {
		return repository.findBySourcePlanetIdAndMissionIsNull(targetPlanet.getId());
	}

	/**
	 * Saves the obtained unit to the database <br>
	 * <b>IMPORTANT:</b> it may change the id, use the resultant value <br>
	 * Will add the count to existing one, <b>if it exists in the planet</b>
	 * 
	 * @param userId
	 * @param obtainedUnit
	 *            <b>NOTICE:</b> Won't be changed from inside
	 * @return new instance of saved obtained unit
	 * @author Kevin Guanche Darias
	 */
	public ObtainedUnit saveWithAdding(Integer userId, ObtainedUnit obtainedUnit) {
		ObtainedUnit retVal;
		if (isDeployedInUserPlanet(obtainedUnit)) {
			ObtainedUnit existingOne = findOneByUserIdAndUnitIdAndSourcePlanetIdAndItNot(userId,
					obtainedUnit.getUnit().getId(), obtainedUnit.getSourcePlanet().getId(), obtainedUnit.getId());
			if (existingOne == null) {
				retVal = save(obtainedUnit);
			} else {
				existingOne.setCount(existingOne.getCount() + obtainedUnit.getCount());
				retVal = save(existingOne);
				if (obtainedUnit.getId() != null) {
					delete(obtainedUnit);
				}
			}
		} else {
			throw new SgtBackendNotImplementedException("This type of action is not actually implemented");
		}
		return retVal;
	}

	/**
	 * Saves the Obtained unit with subtraction
	 * 
	 * @param obtainedUnit
	 *            Target obtained unit
	 * @param substractionCount
	 *            Count to subtract
	 * @return saved obtained unit, null if the count is the same
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObtainedUnit saveWithSubtraction(ObtainedUnit obtainedUnit, Long substractionCount) {
		if (substractionCount > obtainedUnit.getCount()) {
			throw new SgtBackendInvalidInputException(
					"Can't not subtract because, obtainedUnit count is less than the amount to subtract");
		} else if (obtainedUnit.getCount() > substractionCount) {
			obtainedUnit.setCount(obtainedUnit.getCount() - substractionCount);
			return save(obtainedUnit);
		} else if (obtainedUnit.getCount() == substractionCount) {
			delete(obtainedUnit);
			return null;
		} else {
			throw new ProgrammingException("Should never ever happend");
		}
	}

	/**
	 * Searches an ObtainedUnit in <i>storage</i> having an unit with the
	 * <b>same id</b> than <i>searchValue</i>
	 * 
	 * @param storage
	 *            List that will be searched through
	 * @param searchValue
	 *            Value that is going to be search inside <i>storage</i>
	 * @return ObtainedUnit found or <b>null if NOT found</b>
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObtainedUnit findHavingSameUnit(List<ObtainedUnit> storage, ObtainedUnit searchValue) {
		return storage.stream().filter(currentUnit -> searchValue.getUnit().getId() == currentUnit.getUnit().getId())
				.findFirst().orElse(null);
	}

	public Long deleteByMissionId(Long missionId) {
		return repository.deleteByMissionId(missionId);
	}

	public List<ObtainedUnit> findInMyPlanet(Long planetId) {
		userStorageBo.checkOwnPlanet(planetId);
		return repository.findBySourcePlanetIdAndMissionNull(planetId);
	}

	/**
	 * Finds the involved units in an attack
	 * 
	 * @todo In the future find too the deployed units, and discard the ones in
	 *       return missions (we don't want to kill people in return state)
	 * @param attackedPlanet
	 * @param attackMission
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<ObtainedUnit> findInvolvedInAttack(Planet attackedPlanet, Mission attackMission) {
		List<ObtainedUnit> retVal = new ArrayList<>();
		retVal.addAll(repository.findBySourcePlanetId(attackedPlanet.getId()));
		retVal.addAll(repository.findByTargetPlanetIdAndMissionIdNotNullAndMissionIdNot(attackedPlanet.getId(),
				attackMission.getId()));
		return retVal;
	}

	public Long deleteBySourcePlanetIdAndMissionIdNull(Planet sourcePlanet) {
		return repository.deleteBySourcePlanetIdAndMissionIdNull(sourcePlanet.getId());
	}

	public boolean existsByMission(Mission mission) {
		return repository.findOneByMission(mission) != null;
	}

	private boolean isDeployedInUserPlanet(ObtainedUnit obtainedUnit) {
		return obtainedUnit.getSourcePlanet() != null && obtainedUnit.getTargetPlanet() == null;
	}

}
