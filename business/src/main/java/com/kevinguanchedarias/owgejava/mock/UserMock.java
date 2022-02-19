package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserMock {
    public static final int USER_ID_1 = 122;
    public static final int USER_ID_2 = 118273;

    public static UserStorage givenUser1() {
        var user = new UserStorage();
        user.setId(USER_ID_1);
        return user;
    }

    public static UserStorage givenUser2() {
        var user = new UserStorage();
        user.setId(USER_ID_2);
        return user;
    }
}
