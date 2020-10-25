package com.kevinguanchedarias.owgejava.rest.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;

/**
 *
 * @since 0.9.5
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("game/twitch-state")
@ApplicationScope
public class TwitchStateRestService {
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
			socketIoService.sendMessage(null, "twitch_state_change", () -> statusBool);
		} else {
			throw new SgtBackendInvalidInputException("You can't get out of Matrix, the system rules your live!");
		}
	}
}
