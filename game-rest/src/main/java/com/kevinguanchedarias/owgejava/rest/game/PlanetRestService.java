package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("game/planet")
@ApplicationScope
@AllArgsConstructor
public class PlanetRestService implements SyncSource {

    private final PlanetRepository planetRepository;
    private final PlanetBo planetBo;
    private final UserSessionService userSessionService;

    @PostMapping("leave")
    public String leave(@RequestParam("planetId") Long planetId) {
        planetBo.doLeavePlanet(userSessionService.findLoggedIn().getId(), planetId);
        return "\"OK\"";
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create()
                .withHandler("planet_owned_change", user -> planetBo.toDto(planetRepository.findByOwnerId(user.getId()))).build();
    }
}
