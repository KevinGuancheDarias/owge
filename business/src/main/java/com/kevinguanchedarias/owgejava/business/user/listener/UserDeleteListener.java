package com.kevinguanchedarias.owgejava.business.user.listener;

import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface UserDeleteListener {
    /**
     * The order in which to run the listener been 0 the first priority, and  {@link Integer.MAX_VALUE} the latest priority
     */
    int order();

    void doDeleteUser(UserStorage user);
}
