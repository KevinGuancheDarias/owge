package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.stereotype.Component;

@Component
public class FakeUserDeleteListener implements UserDeleteListener {

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        //  Nothing as will be stubbed
    }
}
