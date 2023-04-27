package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserMock {
    public static final int USER_ID_1 = 122;
    public static final String USER_1_NAME = "user_" + USER_ID_1;
    public static final int USER_ID_2 = 118273;
    public static final String USER_2_NAME = "user_" + USER_ID_2;

    public static UserStorage givenUser(Integer id) {
        var user = new UserStorage();
        user.setId(id);
        user.setUsername("user_" + id);
        return user;
    }

    public static UserStorage givenUser1() {
        return givenUser(USER_ID_1);
    }

    public static UserStorage givenUser2() {
        return givenUser(USER_ID_2);
    }
}
