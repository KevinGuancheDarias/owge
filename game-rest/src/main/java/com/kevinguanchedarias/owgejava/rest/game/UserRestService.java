package com.kevinguanchedarias.owgejava.rest.game;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

@RestController
@RequestMapping("game/user")
@ApplicationScope
public class UserRestService implements SyncSource {

	@Autowired
	private UserStorageBo userStorageBo;

	@GetMapping("exists")
	public Object exists() {
		return userStorageBo.exists(userStorageBo.findLoggedIn().getId());
	}

	/**
	 * Will subscribe the user to this universe
	 *
	 * @return If everything well ok, returns true
	 * @author Kevin Guanche Darias
	 */
	@GetMapping("subscribe")
	public Object subscribe(@RequestParam("factionId") Integer factionId) {
		return userStorageBo.subscribe(factionId);
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("user_data_change", this::findData).build();
	}

	private Object findData(UserStorage user) {
		UserStorage withDetails = userStorageBo.findById(user.getId());
		return userStorageBo.findData(withDetails);
	}
}
