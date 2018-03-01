package com.kevinguanchedarias.sgtjava.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
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

	public ObtainedUnit findOneByUserIdAndUnitId(Integer userId, Integer unitId) {
		return repository.findOneByUserIdAndUnitId(userId, unitId);
	}

	public Long countByUserAndUnitId(UserStorage user, Integer unitId) {
		return repository.countByUserIdAndUnitId(user.getId(), unitId);
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

	public Long deleteByMissionId(Long missionId) {
		return repository.deleteByMissionId(missionId);
	}

	public List<ObtainedUnit> findInMyPlanet(Long planetId) {
		userStorageBo.checkOwnPlanet(planetId);
		return repository.findBySourcePlanetId(planetId);
	}

	private boolean isDeployedInUserPlanet(ObtainedUnit obtainedUnit) {
		return obtainedUnit.getSourcePlanet() != null && obtainedUnit.getTargetPlanet() == null;
	}

}
