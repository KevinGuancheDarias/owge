package com.kevinguanchedarias.sgtjava.business;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.kevinguanchedarias.sgtjava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.sgtjava.dto.MissionDto;
import com.kevinguanchedarias.sgtjava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.MissionReport;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.MissionType;
import com.kevinguanchedarias.sgtjava.exception.NotFoundException;
import com.kevinguanchedarias.sgtjava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.exception.UserNotFoundException;
import com.kevinguanchedarias.sgtjava.pojo.DeliveryQueueEntry;
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

	@Autowired
	private MissionReportBo missionReportBo;

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
		List<ObtainedUnit> obtainedUnits = new ArrayList<>();
		UnitMissionInformation targetMissionInformation = copyMissionInformation(missionInformation);
		UserStorage user = userStorageBo.findLoggedIn();
		targetMissionInformation.setUserId(user.getId());
		checkAndLoadObtainedUnits(missionInformation);
		Mission mission = missionRepository
				.saveAndFlush((prepareMission(targetMissionInformation, MissionType.EXPLORE)));
		targetMissionInformation.getInvolvedUnits().forEach(current -> {
			ObtainedUnit currentObtainedUnit = new ObtainedUnit();
			currentObtainedUnit.setMission(mission);
			currentObtainedUnit.setCount(current.getCount());
			currentObtainedUnit.setUser(user);
			currentObtainedUnit.setUnit(unitBo.findById(current.getId()));
			currentObtainedUnit.setSourcePlanet(mission.getTargetPlanet());
			currentObtainedUnit.setTargetPlanet(mission.getSourcePlanet());
			obtainedUnits.add(currentObtainedUnit);
		});
		obtainedUnitBo.save(obtainedUnits);
		scheduleMission(mission);
		return new UnitRunningMissionDto(mission, obtainedUnits);
	}

	/**
	 * Parses the exploration of a planet
	 * 
	 * @param missionId
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void processExplore(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = obtainedUnitBo.findByMissionId(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		if (!planetBo.isExplored(user, targetPlanet)) {
			planetBo.defineAsExplored(user, targetPlanet);
		}
		List<ObtainedUnit> unitsInPlanet = obtainedUnitBo.explorePlanetUnits(targetPlanet);
		adminRegisterReturnMission(mission);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
				.withExploredInformation(unitsInPlanet);
		MissionReport missionReport = new MissionReport("{}", mission);
		missionReport.setUser(user);
		missionReport = missionReportBo.save(missionReport);
		missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
		mission.setReport(missionReport);
		resolveMission(mission);
		socketIoService.sendMessage(user, "explore_report", builder.build());
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
		List<ObtainedUnit> inPlanet = obtainedUnitBo.findByUserIdAndSourcePlanetAndMissionIdIsNull(mission.getUser(),
				mission.getTargetPlanet());
		obtainedUnits.forEach(current -> {
			ObtainedUnit existingUnit = obtainedUnitBo.findHavingSameUnit(inPlanet, current);
			if (existingUnit == null) {
				current.setMission(null);
				current.setSourcePlanet(mission.getTargetPlanet());
				current.setTargetPlanet(null);
			} else {
				existingUnit.addCount(current.getCount());
				obtainedUnitBo.delete(current);
			}
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
	 * <li>Check for each selected unit if there is an associated obtainedUnit
	 * and if count is valid</li>
	 * </ul>
	 * 
	 * @param missionInformation
	 * @return Database list of <i>ObtainedUnit</i> with the subtraction
	 *         <b>already applied</b>
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
			if (current.getCount() == null) {
				throw new SgtBackendInvalidInputException("No count was specified for unit " + current.getId());
			}
			ObtainedUnit currentObtainedUnit = findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMissionIdIsNull(
					missionInformation.getUserId(), current.getId(), missionInformation.getSourcePlanetId());
			retVal.add(obtainedUnitBo.saveWithSubtraction(currentObtainedUnit, current.getCount()));
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
	 * Checks if the input Unit <i>id</i> exists, and returns the associated
	 * ObtainedUnit
	 * 
	 * @param id
	 * @return the expected obtained id
	 * @throws NotFoundException
	 *             If obtainedUnit doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private ObtainedUnit findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMissionIdIsNull(Integer userId, Integer unitId,
			Long planetId) {
		ObtainedUnit retVal = obtainedUnitBo.findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIdIsNull(userId, unitId,
				planetId);
		if (retVal == null) {
			throw new NotFoundException("No obtainedUnit for unit with id " + unitId + " was found in planet "
					+ planetId + ", nice try, dirty hacker!");
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
	private CompletableFuture<DeliveryQueueEntry> emitLocalMissionChange(Mission mission, UserStorage user) {
		return socketIoService.sendMessage(user, "local_mission_change",
				dtoUtilService.dtoFromEntity(MissionDto.class, mission));
	}
}
