package com.kevinguanchedarias.owgejava.rest.game;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

@RestController
@RequestMapping("game/planet")
@ApplicationScope
public class PlanetRestService implements SyncSource {

	@Autowired
	private PlanetBo planetBo;

	@Autowired
	private UserStorageBo userStorageBo;

	@PostMapping("leave")
	public String leave(@RequestParam("planetId") Long planetId) {
		planetBo.doLeavePlanet(userStorageBo.findLoggedIn().getId(), planetId);
		return "\"OK\"";
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create()
				.withHandler("planet_owned_change", user -> planetBo.toDto(planetBo.findPlanetsByUser(user))).build();
	}
}
