package com.kevinguanchedarias.owgejava.entity.projection;

import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface AuditDataProjection {
    UserStorage getUser();

    String getIpv4();

    String getIpv6();

    String getUserAgent();

    String getCookie();
}
