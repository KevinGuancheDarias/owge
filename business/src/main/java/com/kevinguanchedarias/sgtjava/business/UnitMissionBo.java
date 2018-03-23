package com.kevinguanchedarias.sgtjava.business;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.kevinguanchedarias.sgtjava.dto.MissionDto;
import com.kevinguanchedarias.sgtjava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.MissionType;
import com.kevinguanchedarias.sgtjava.exception.NotFoundException;
import com.kevinguanchedarias.sgtjava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.exception.UserNotFoundException;
import com.kevinguanchedarias.sgtjava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.sgtjava.util.DtoUtilService;

@Service
public class UnitMissionBo extends AbstractMissionBo {
	private static final long serialVersionUID = 344402831344882216L;

	private static final Logger LOG = Logger.getLogger(UnitMissionBo.class);
	private static final String JOB_GROUP_NAME = "UnitMissions";

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private SocketIoService socketIoService;

	private DtoUtilService dtoUtilService = new DtoUtilService();

	@Override
	public String getGroupName() {
		return JOB_GROUP_NAME;
	}

	@Override
	public Logger getLogger() {
		return LOG;
	}

	/**
	 * Registers a explore mission <b>as logged in user</b>
	 * 
	 * @param missionInformation
	 *            <i>userId</i> is <b>ignored</b> in this method <b>immutable
	 *            object</b>
	 * @return mission representation DTO
	 * @throws SgtBackendInvalidInputException
	 *             When input information is not valid
	 * @throws UserNotFoundException
	 *             When user doesn't exists <b>(in this universe)</b>
	 * @throws PlanetNotFoundException
	 *             When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public UnitRunningMissionDto myRegisterExploreMission(UnitMissionInformation missionInformation) {
		if (missionInformation.getUserId() == null) {
			missionInformation.setUserId(userStorageBo.findLoggedIn().getId());
		} else {
			checkInvokerIsTheLoggedUser(missionInformation.getUserId());
		}
		return adminRegisterExploreMission(missionInformation);
	}

	/**
	 * Registers a explore mission <b>as a admin</b>
	 * 
	 * @param missionInformation
	 * @return mission representation DTO
	 * @throws SgtBackendInvalidInputException
	 *             When input information is not valid
	 * @throws UserNotFoundException
	 *             When user doesn't exists <b>(in this universe)</b>
	 * @throws PlanetNotFoundException
	 *             When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public UnitRunningMissionDto adminRegisterExploreMission(UnitMissionInformation missionInformation) {
		UnitMissionInformation targetMissionInformation = copyMissionInformation(missionInformation);
		targetMissionInformation.setUserId(userStorageBo.findLoggedIn().getId());
		List<ObtainedUnit> obtainedUnits = checkAndLoadObtainedUnits(missionInformation);
		Mission mission = missionRepository
				.saveAndFlush((prepareMission(targetMissionInformation, MissionType.EXPLORE)));
		obtainedUnits.forEach(current -> {
			current.setMission(mission);
			current.setSourcePlanet(mission.getTargetPlanet());
			current.setTargetPlanet(mission.getSourcePlanet());
		});
		obtainedUnitBo.save(obtainedUnits);
		scheduleMission(mission);
		return new UnitRunningMissionDto(mission, obtainedUnits);
	}

	/**
	 * Parses the exploration of a planet
	 * 
	 * @todo in the future generate a report with the explore result
	 * @param missionId
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void processExplore(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		Planet targetPlanet = mission.getTargetPlanet();
		if (!planetBo.isExplored(user, targetPlanet)) {
			planetBo.defineAsExplored(user, targetPlanet);
		}
		adminRegisterReturnMission(mission);
		resolveMission(mission);
		emitLocalMissionChange(mission, user);
	}

	/**
	 * Creates a return mission from an existing mission
	 * 
	 * @param mission
	 *            Existing mission that will be returned
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void adminRegisterReturnMission(Mission mission) {
		Mission returnMission = new Mission();
		returnMission.setType(findMissionType(MissionType.RETURN_MISSION));
		returnMission.setRequiredTime(mission.getRequiredTime());
		returnMission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
		returnMission.setSourcePlanet(mission.getTargetPlanet());
		returnMission.setTargetPlanet(mission.getSourcePlanet());
		returnMission.setUser(mission.getUser());
		returnMission.setRelatedMission(mission);
		List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findByMissionId(mission.getId());
		missionRepository.saveAndFlush(returnMission);
		obtainedUnits.forEach(current -> current.setMission(returnMission));
		obtainedUnitBo.save(obtainedUnits);
		scheduleMission(returnMission);
	}

	@Transactional
	public void proccessReturnMission(Long missionId) {
		Mission mission = missionRepository.findOne(missionId);
		List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findByMissionId(mission.getId());
		obtainedUnits.forEach(current -> {
			current.setMission(null);
			current.setSourcePlanet(mission.getTargetPlanet());
			current.setTargetPlanet(null);
		});
		resolveMission(mission);
		emitLocalMissionChange(mission, mission.getUser());
	}

	/**
	 * Will check if the input DTO is valid, the following validations will be
	 * done <br>
	 * <b>IMPORTANT:</b> This method is intended to be use as part of the
	 * mission registration process
	 * <ul>
	 * <li>Check if the user exists</li>
	 * <li>Check if the sourcePlanet exists</li>
	 * <li>Check if the targetPlanet exists</li>
	 * <li>Check for each obtained unit if the id exists</li>
	 * <li>Check for each obtained unit if it doesn't have already a
	 * mission</li>
	 * <li>Check for each obtained unit if they belongs to <i>userId</i></li>
	 * <li>Check if the obtained units they belong to the sourcePlanet</li>
	 * </ul>
	 * 
	 * @param missionInformation
	 * @return Database list of <i>ObtainedUnit</i>
	 * @throws SgtBackendInvalidInputException
	 *             when validation was not passed
	 * @throws UserNotFoundException
	 *             When user doesn't exists <b>(in this universe)</b>
	 * @throws PlanetNotFoundException
	 *             When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private List<ObtainedUnit> checkAndLoadObtainedUnits(UnitMissionInformation missionInformation) {
		List<ObtainedUnit> retVal = new ArrayList<>();
		checkUserExists(missionInformation.getUserId());
		checkPlanetExists(missionInformation.getSourcePlanetId());
		checkPlanetExists(missionInformation.getTargetPlanetId());
		if (CollectionUtils.isEmpty(missionInformation.getInvolvedUnits())) {
			throw new SgtBackendInvalidInputException("involvedUnits can't be empty");
		}
		missionInformation.getInvolvedUnits().forEach(current -> {
			ObtainedUnit currentObtainedUnit = findObtainedUnit(current.getId());
			if (currentObtainedUnit.getMission() != null) {
				throw new SgtBackendInvalidInputException("obtainedUnit already involved in mission");
			}
			if (!currentObtainedUnit.getUser().getId().equals(missionInformation.getUserId())) {
				throw new SgtBackendInvalidInputException("obtainedUnit doesn't belong to invoker user");
			}
			if (!currentObtainedUnit.getSourcePlanet().getId().equals(missionInformation.getSourcePlanetId())) {
				throw new SgtBackendInvalidInputException("obtainedUnit doesn't belong to sourcePlanet");
			}
			retVal.add(currentObtainedUnit);
		});
		return retVal;
	}

	/**
	 * Returns a copy of the object, used to make missionInformation immutable
	 * 
	 * @param missionInformation
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private UnitMissionInformation copyMissionInformation(UnitMissionInformation missionInformation) {
		UnitMissionInformation retVal = new UnitMissionInformation();
		BeanUtils.copyProperties(missionInformation, retVal);
		return retVal;
	}

	/**
	 * Checks if the input <i>id</i> exists, and returns it if found
	 * 
	 * @param id
	 * @return the expected obtained id
	 * @throws NotFoundException
	 *             If obtainedUnit doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private ObtainedUnit findObtainedUnit(Long id) {
		ObtainedUnit retVal = obtainedUnitBo.findById(id);
		if (retVal == null) {
			throw new NotFoundException("No obtainedUnit with id " + id + " was found, nice try, dirty hacker!");
		}
		return retVal;
	}

	/**
	 * Checks if the logged in user is the creator of the mission
	 * 
	 * @param invoker
	 *            The creator of the mission
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void checkInvokerIsTheLoggedUser(Integer invoker) {
		if (!invoker.equals(userStorageBo.findLoggedIn().getId())) {
			throw new SgtBackendInvalidInputException("Invoker is not the logged in user");
		}
	}

	/**
	 * Prepares a mission to be scheduled
	 * 
	 * @param missionInformation
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Mission prepareMission(UnitMissionInformation missionInformation, MissionType type) {
		Mission retVal = new Mission();
		Double requiredTime = calculateRequiredTime(type);
		retVal.setMissionInformation(null);
		retVal.setType(findMissionType(type));
		retVal.setUser(userStorageBo.findById(missionInformation.getUserId()));
		retVal.setRequiredTime(requiredTime);
		Long sourcePlanetId = missionInformation.getSourcePlanetId();
		Long targetPlanetId = missionInformation.getTargetPlanetId();
		if (sourcePlanetId != null) {
			retVal.setSourcePlanet(planetBo.findById(sourcePlanetId));
		}
		if (targetPlanetId != null) {
			retVal.setTargetPlanet(planetBo.findById(targetPlanetId));
		}

		retVal.setTerminationDate(computeTerminationDate(requiredTime));
		return retVal;
	}

	/**
	 * Calculates time required to complete the mission
	 * 
	 * @todo In the future calculate the units speed
	 * 
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Double calculateRequiredTime(MissionType type) {
		return Double.valueOf(configurationBo.findMissionBaseTimeByType(type));
	}

	/**
	 * Emits a local mission change to the target user
	 * 
	 * @param mission
	 * @param user
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void emitLocalMissionChange(Mission mission, UserStorage user) {
		socketIoService.sendMessage(user, "local_mission_change",
				dtoUtilService.dtoFromEntity(MissionDto.class, mission));
	}
}
