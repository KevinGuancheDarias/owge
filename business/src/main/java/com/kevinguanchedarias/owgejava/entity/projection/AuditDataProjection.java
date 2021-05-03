package com.kevinguanchedarias.owgejava.entity.projection;

import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface AuditDataProjection {
    UserStorage getUser();
    String getIp();
    String getUserAgent();
    String getCookie();
}
