package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

	@GetMapping("findMyPlanets")
	public List<PlanetDto> findMyPlanets() {
		return planetBo.toDto(planetBo.findMyPlanets());
	}

	@PostMapping("leave")
	public String leave(@RequestParam("planetId") Long planetId) {
		planetBo.doLeavePlanet(userStorageBo.findLoggedIn().getId(), planetId);
		return "\"OK\"";
	}
}
