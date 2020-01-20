package com.kevinguanchedarias.owgejava.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

@RestController
@RequestMapping("game/user")
@ApplicationScope
public class UserRestService {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	@RequestMapping(value = "exists", method = RequestMethod.GET)
	public Object exists() {
		return userStorageBo.exists(userStorageBo.findLoggedIn().getId());
	}

	/**
	 * Will subscribe the user to this universe
	 * 
	 * @return If everything well ok, returns true
	 * @author Kevin Guanche Darias
	 */
	@RequestMapping(value = "subscribe", method = RequestMethod.GET)
	public Object subscribe(@RequestParam("factionId") Integer factionId) {
		return userStorageBo.subscribe(factionId);
	}

	@RequestMapping(value = "findData", method = RequestMethod.GET)
	public Object findData() {
		UserStorage user = userStorageBo.findLoggedInWithDetails(true);
		UserStorageDto userDto = new UserStorageDto();
		userDto.dtoFromEntity(user);
		userDto.setFactionDto(EntityPojoConverterUtil.convertFromTo(FactionDto.class, user.getFaction()));
		userDto.setHomePlanetDto(EntityPojoConverterUtil.convertFromTo(PlanetDto.class, user.getHomePlanet()));
		userDto.setAlliance(dtoUtilService.dtoFromEntity(AllianceDto.class, user.getAlliance()));

		Galaxy galaxyData = user.getHomePlanet().getGalaxy();
		userDto.getHomePlanetDto().setGalaxyId(galaxyData.getId());
		userDto.getHomePlanetDto().setGalaxyName(galaxyData.getName());
		userDto.setConsumedEnergy(userStorageBo.findConsumedEnergy(user));
		userDto.setMaxEnergy(userStorageBo.findMaxEnergy(user));
		return userDto;
	}
}
