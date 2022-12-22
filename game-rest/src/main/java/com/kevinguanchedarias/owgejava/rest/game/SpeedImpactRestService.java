package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnlockedSpeedImpactGroupService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Map;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@RestController
@RequestMapping("game/speed-impact-group")
@ApplicationScope
@AllArgsConstructor
public class SpeedImpactRestService implements SyncSource {

    private final UnlockedSpeedImpactGroupService unlockedSpeedImpactGroupService;

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("speed_impact_group_unlocked_change",
                unlockedSpeedImpactGroupService::findCrossGalaxyUnlocked).build();
    }
}
