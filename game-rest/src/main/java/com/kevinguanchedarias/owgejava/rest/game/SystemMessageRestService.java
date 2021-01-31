package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.SystemMessageBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("game/system-message")
@ApplicationScope
public class SystemMessageRestService implements SyncSource {
	@Autowired
	private SystemMessageBo bo;

	@Autowired
	private UserStorageBo userStorageBo;

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("system_message_change", user -> bo.findReadByUser(user.getId()))
				.build();
	}

	@PostMapping("mark-as-read")
	public void markAsRead(@RequestBody List<Integer> messages) {
		bo.markAsRead(messages, userStorageBo.findLoggedInWithDetails());
	}

}
