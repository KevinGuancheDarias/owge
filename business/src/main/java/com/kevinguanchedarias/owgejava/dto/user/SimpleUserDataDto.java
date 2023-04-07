package com.kevinguanchedarias.owgejava.dto.user;


import com.kevinguanchedarias.owgejava.entity.UserStorage;

public record SimpleUserDataDto(int id, String username, String email) {
    public static SimpleUserDataDto of(UserStorage user) {
        return new SimpleUserDataDto(user.getId(), user.getUsername(), user.getEmail());
    }
}
