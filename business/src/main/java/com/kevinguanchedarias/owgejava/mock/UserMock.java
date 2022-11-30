package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserMock {
    public static final int USER_ID_1 = 122;
    public static final int USER_ID_2 = 118273;

    public static UserStorage givenUser(int id) {
        var user = new UserStorage();
        user.setId(id);
        return user;
    }

    public static UserStorage givenUser1() {
        return givenUser(USER_ID_1);
    }

    public static UserStorage givenUser2() {
        return givenUser(USER_ID_2);
    }
}
