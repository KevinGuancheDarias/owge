package com.kevinguanchedarias.owgejava.event;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.FilterEventHandler;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;

public class ResourceAutoUpdateEventHandler implements FilterEventHandler {

	@Autowired
	private UserStorageBo userStorageBo;

	@Override
	public void doBefore() {
		// Does nothing before authenticating!
	}

	@Override
	public void doAfter() {
		var user = userStorageBo.findLoggedIn();
		if (userStorageBo.exists(user)) {
			if (userStorageBo.isBanned(user.getId())) {
				throw new AccessDeniedException("I18N_ERR_BANNED");
			}
			userStorageBo.triggerResourcesUpdate(user.getId());
		}
	}

}
