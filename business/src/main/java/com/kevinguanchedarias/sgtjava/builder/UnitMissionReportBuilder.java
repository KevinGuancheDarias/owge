package com.kevinguanchedarias.sgtjava.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.sgtjava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.sgtjava.dto.PlanetDto;
import com.kevinguanchedarias.sgtjava.dto.UserStorageDto;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.exception.CommonException;
import com.kevinguanchedarias.sgtjava.util.DtoUtilService;

public class UnitMissionReportBuilder {

	private Map<String, Object> createdMap = new HashMap<>();
	private DtoUtilService dtoUtilService = new DtoUtilService();
	private ObjectMapper mapper = new ObjectMapper();

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

	public Map<String, Object> buildClone() {
		Map<String, Object> target = new HashMap<>();
		BeanUtils.copyProperties(createdMap, target);
		return target;
	}

	public String buildJson() {
		try {
			return mapper.writeValueAsString(createdMap);
		} catch (JsonProcessingException e) {
			throw new CommonException("Could not convert object to JSON", e);
		}
	}

	public UnitMissionReportBuilder withInvolvedUnits(List<ObtainedUnit> selectedUnits) {
		createdMap.put("involvedUnits", obtainedUnitToDto(selectedUnits));
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

	public UnitMissionReportBuilder withExploredInformation(List<ObtainedUnit> unitsInPlanet) {
		createdMap.put("unitsInPlanet", obtainedUnitToDto(unitsInPlanet));
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

	private List<ObtainedUnitDto> obtainedUnitToDto(List<ObtainedUnit> units) {
		return dtoUtilService.convertEntireArray(ObtainedUnitDto.class, units).stream().map(current -> {
			current.getUnit().setImprovement(null);
			current.setSourcePlanet(null);
			current.setTargetPlanet(null);
			current.setMission(null);
			return current;
		}).collect(Collectors.toList());
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
