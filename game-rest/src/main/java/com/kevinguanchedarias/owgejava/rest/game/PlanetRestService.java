package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

@RestController
@RequestMapping("game/planet")
@ApplicationScope
public class PlanetRestService implements SyncSource {

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

	@Override
	public Map<String, Supplier<Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("planet_owned_change", this::findMyPlanets).build();
	}
}
