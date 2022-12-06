package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;

@UtilityClass
public class TokenUserMock {
    public static final int TOKEN_USER_ID = USER_ID_1;
    public static final String TOKEN_USER_EMAIL = "Foo@bar.com";
    public static final String TOKEN_USERNAME = "foo";

    public static TokenUser givenTokenUser() {
        return givenTokenUser(TOKEN_USER_ID);
    }

    public static TokenUser givenTokenUser(int id) {
        var tokenUser = new TokenUser();
        tokenUser.setId(id);
        tokenUser.setEmail(TOKEN_USER_EMAIL);
        tokenUser.setUsername(TOKEN_USERNAME);
        return tokenUser;
    }
}
