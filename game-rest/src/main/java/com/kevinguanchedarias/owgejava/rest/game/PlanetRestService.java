package com.kevinguanchedarias.owgejava.rest.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;

@RestController
@RequestMapping("game/planet")
@ApplicationScope
public class PlanetRestService {

	@Autowired
	private PlanetBo planetBo;

	@Autowired
	private UserStorageBo userStorageBo;

	@RequestMapping(value = "findMyPlanets", method = RequestMethod.GET)
	public Object findMyPlanets() {
		PlanetDto planetDto = new PlanetDto();
		return planetDto.dtoFromEntity(PlanetDto.class, planetBo.findMyPlanets());
	}

	@RequestMapping(value = "leave", method = RequestMethod.POST)
	public String leave(@RequestParam("planetId") Long planetId) {
		planetBo.doLeavePlanet(userStorageBo.findLoggedIn().getId(), planetId);
		return "OK";
	}
}
