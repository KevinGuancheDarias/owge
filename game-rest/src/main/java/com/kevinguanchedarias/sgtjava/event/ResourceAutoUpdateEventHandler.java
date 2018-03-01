package com.kevinguanchedarias.sgtjava.event;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.FilterEventHandler;
import com.kevinguanchedarias.sgtjava.business.UserStorageBo;

public class ResourceAutoUpdateEventHandler implements FilterEventHandler {

	@Autowired
	private UserStorageBo userStorageBo;

	@Override
	public void doBefore() {
		// Does nothing before authenticating!
	}

	@Override
	public void doAfter() {
		if (userStorageBo.exists(userStorageBo.findLoggedIn())) {
			userStorageBo.triggerResourcesUpdate();
		}
	}

}
