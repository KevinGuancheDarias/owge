package com.kevinguanchedarias.owgejava.builder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo.AttackInformation;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnitMissionReportBuilder {

	private final Map<String, Object> createdMap = new HashMap<>();
	private final DtoUtilService dtoUtilService = new DtoUtilService();
	private final ObjectMapper mapper = new ObjectMapper();

	public static UnitMissionReportBuilder create(UserStorage user, Planet sourcePlanet, Planet targetPlanet,
												  List<ObtainedUnit> selectedUnits) {
		return create().withSenderUser(user).withSourcePlanet(sourcePlanet).withTargetPlanet(targetPlanet)
				.withInvolvedUnits(selectedUnits);
	}

	public static UnitMissionReportBuilder create() {
		return new UnitMissionReportBuilder();
	}

	/**
	 * Builds the object
	 *
	 * @return instance of created map <b>NOT</b> cloned
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Map<String, Object> build() {
		return createdMap;
	}

	public String buildJson() {
		try {
			return mapper.writeValueAsString(createdMap);
		} catch (JsonProcessingException e) {
			throw new CommonException("Could not convert object to JSON", e);
		}
	}

	public UnitMissionReportBuilder withInvolvedUnits(List<ObtainedUnit> selectedUnits) {
		if (selectedUnits != null) {
			createdMap.put("involvedUnits", obtainedUnitToDto(selectedUnits));
		}
		return this;
	}

	public UnitMissionReportBuilder withSourcePlanet(Planet planet) {
		createdMap.put("sourcePlanet", planetToDto(planet));
		return this;
	}

	public UnitMissionReportBuilder withTargetPlanet(Planet planet) {
		createdMap.put("targetPlanet", planetToDto(planet));
		return this;
	}

	public UnitMissionReportBuilder withSenderUser(UserStorage user) {
		createdMap.put("senderUser", userToDto(user));
		return this;
	}

	public UnitMissionReportBuilder withId(Long id) {
		createdMap.put("id", id);
		return this;
	}

	public UnitMissionReportBuilder withExploredInformation(List<ObtainedUnitDto> unitsInPlanet) {
		createdMap.put("unitsInPlanet", handleAllDtos(unitsInPlanet));
		return this;
	}

	public UnitMissionReportBuilder withGatherInformation(Double primaryResource, Double secondaryResource) {
		createdMap.put("gatheredPrimary", primaryResource);
		createdMap.put("gatheredSecondary", secondaryResource);
		return this;
	}

	public UnitMissionReportBuilder withEstablishBaseInformation(Boolean status) {
		return withEstablishBaseInformation(status, "");
	}

	public UnitMissionReportBuilder withEstablishBaseInformation(Boolean status, String statusStr) {
		createdMap.put("establishBaseStatus", status);
		createdMap.put("establishBaseStatusStr", statusStr);
		return this;
	}

	public UnitMissionReportBuilder withAttackInformation(AttackInformation attackInformation) {
		List<Map<String, Object>> attackInformationMap = new ArrayList<>();
		attackInformation.getUsers().entrySet().forEach(currentUser -> {
			Map<String, Object> userMap = new HashMap<>();
			userMap.put("userInfo", userToDto(currentUser.getValue().getUser()));
			userMap.put("earnedPoints", currentUser.getValue().getEarnedPoints());
			List<Map<String, Object>> userUnits = new ArrayList<>();
			currentUser.getValue().getUnits().forEach(currentUnit -> {
				Map<String, Object> unitMap = new HashMap<>();
				unitMap.put("initialCount", currentUnit.getInitialCount());
				unitMap.put("finalCount", currentUnit.getFinalCount());
				unitMap.put("obtainedUnit", obtainedUnitToDto(currentUnit.getObtainedUnit()));
				userUnits.add(unitMap);
			});
			userMap.put("units", userUnits);
			attackInformationMap.add(userMap);
		});
		createdMap.put("attackInformation", attackInformationMap);
		return this;
	}

	public UnitMissionReportBuilder withConquestInformation(Boolean status, String statusStr) {
		createdMap.put("conquestStatus", status);
		createdMap.put("conquestStatusStr", statusStr);
		return this;
	}

	public UnitMissionReportBuilder withErrorInformation(String errorText) {
		createdMap.put("errorText", errorText);
		return this;
	}

	/**
	 * A builder class can't be instantiate
	 *
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private UnitMissionReportBuilder() {
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	private ObtainedUnitDto obtainedUnitToDto(ObtainedUnit unit) {
		return obtainedUnitToDto(dtoUtilService.dtoFromEntity(ObtainedUnitDto.class, unit));
	}

	private ObtainedUnitDto obtainedUnitToDto(ObtainedUnitDto unit) {
		if (unit.getUnit() != null) {
			unit.getUnit().setImprovement(null);
		}
		unit.setSourcePlanet(null);
		unit.setTargetPlanet(null);
		unit.setMission(null);
		return unit;
	}

	private List<ObtainedUnitDto> obtainedUnitToDto(List<ObtainedUnit> units) {
		return units.stream().map(this::obtainedUnitToDto).collect(Collectors.toList());
	}

	private List<ObtainedUnitDto> handleAllDtos(List<ObtainedUnitDto> units) {
		return units.stream().map(this::obtainedUnitToDto).collect(Collectors.toList());
	}

	private PlanetDto planetToDto(Planet planet) {
		return dtoUtilService.dtoFromEntity(PlanetDto.class, planet);
	}

	private UserStorageDto userToDto(UserStorage user) {
		UserStorageDto retVal = new UserStorageDto();
		retVal.setId(user.getId());
		retVal.setUsername(user.getUsername());
		return retVal;
	}

}
