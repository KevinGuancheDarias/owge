package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Map;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.5
 */
@RestController
@RequestMapping("game/twitch-state")
@ApplicationScope
@AllArgsConstructor
public class TwitchStateRestService implements SyncSource {
    private static final String TWITCH_STATE_CHANGE = "twitch_state_change";
    private final ConfigurationBo configurationBo;
    private final SocketIoService socketIoService;
    private final UserSessionService userSessionService;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.5
     */
    @GetMapping
    public boolean findTwitchState() {
        return Boolean.parseBoolean(configurationBo.findOrSetDefault("TWITCH_STATE", "false").getValue());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.5
     */
    @PutMapping
    public void defineState(@RequestBody String status) {
        boolean statusBool = Boolean.parseBoolean(status.replace("\"", ""));
        if (Boolean.TRUE.equals(userSessionService.findLoggedInWithDetails().getCanAlterTwitchState())) {
            configurationBo.saveByKeyAndValue("TWITCH_STATE", String.valueOf(statusBool));
            socketIoService.sendMessage(null, TWITCH_STATE_CHANGE, () -> statusBool);
        } else {
            throw new SgtBackendInvalidInputException("You can't get out of Matrix, the system rules your live!");
        }
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler(TWITCH_STATE_CHANGE, this::findTwitchState).build();
    }
}
