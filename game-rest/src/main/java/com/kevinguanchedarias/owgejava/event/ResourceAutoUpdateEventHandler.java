package com.kevinguanchedarias.owgejava.event;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.FilterEventHandler;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public class ResourceAutoUpdateEventHandler implements FilterEventHandler {

	@Autowired
	private UserStorageBo userStorageBo;

	@Override
	public void doBefore() {
		// Does nothing before authenticating!
	}

	@Override
	public void doAfter() {
		UserStorage user = userStorageBo.findLoggedIn();
		if (userStorageBo.exists(user)) {
			userStorageBo.triggerResourcesUpdate(user.getId());
		}
	}

}
