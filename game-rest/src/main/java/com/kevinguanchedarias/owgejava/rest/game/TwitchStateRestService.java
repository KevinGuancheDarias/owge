package com.kevinguanchedarias.owgejava.rest.game;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

/**
 *
 * @since 0.9.5
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("game/twitch-state")
@ApplicationScope
public class TwitchStateRestService implements SyncSource {
	private static final String TWITCH_STATE_CHANGE = "twitch_state_change";

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private SocketIoService socketIoService;

	@Autowired
	private UserStorageBo userStorageBo;

	/**
	 *
	 * @return
	 * @since 0.9.5
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping
	public boolean findTwitchState() {
		return Boolean.parseBoolean(configurationBo.findOrSetDefault("TWITCH_STATE", "false").getValue());
	}

	/**
	 *
	 * @param status
	 * @since 0.9.5
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PutMapping
	public void defineState(@RequestBody String status) {
		boolean statusBool = Boolean.parseBoolean(status.replace("\"", ""));
		if (Boolean.TRUE.equals(userStorageBo.findLoggedInWithDetails().getCanAlterTwitchState())) {
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
