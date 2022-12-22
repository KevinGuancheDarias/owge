package com.kevinguanchedarias.owgejava.event;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.FilterEventHandler;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResourceAutoUpdateEventHandler implements FilterEventHandler {

    private final UserStorageBo userStorageBo;
    private final UserStorageRepository userStorageRepository;
    private final UserSessionService userSessionService;

    @Override
    public void doBefore() {
        // Does nothing before authenticating!
    }

    @Override
    public void doAfter() {
        var user = userSessionService.findLoggedIn();
        if (userStorageBo.exists(user)) {
            if (userStorageRepository.isBanned(user.getId())) {
                throw new AccessDeniedException("I18N_ERR_BANNED");
            }
            userStorageBo.triggerResourcesUpdate(user.getId());
        }
    }

}
