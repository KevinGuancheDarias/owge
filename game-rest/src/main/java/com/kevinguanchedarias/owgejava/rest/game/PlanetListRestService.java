package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.PlanetListBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.pojo.PlanetListAddRequestBody;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Map;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@RestController
@RequestMapping("game/planet-list")
@ApplicationScope
@AllArgsConstructor
public class PlanetListRestService implements SyncSource {
    private final PlanetListBo planetListBo;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostMapping
    public void add(@RequestBody PlanetListAddRequestBody body) {
        planetListBo.myAdd(body.getPlanetId(), body.getName());
    }

    @DeleteMapping("{planetId}")
    public void delete(@PathVariable Long planetId) {
        planetListBo.myDelete(planetId);
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("planet_user_list_change",
                user -> planetListBo.findByUserId(user.getId())).build();
    }
}
