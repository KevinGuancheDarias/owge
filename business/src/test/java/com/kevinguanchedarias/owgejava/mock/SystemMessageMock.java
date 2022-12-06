package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.dto.SystemMessageDto;
import com.kevinguanchedarias.owgejava.entity.SystemMessage;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SystemMessageMock {
    public static final int SYSTEM_MESSAGE_ID = 6661122;
    public static final String SYSTEM_MESSAGE_CONTENT = "TheSkyNet";

    public static SystemMessage givenSystemMessage() {
        return SystemMessage.builder()
                .id(SYSTEM_MESSAGE_ID)
                .content(SYSTEM_MESSAGE_CONTENT)
                .build();
    }

    public static SystemMessageDto givenSystemMessageDto() {
        return SystemMessageDto.builder()
                .id(SYSTEM_MESSAGE_ID)
                .content(SYSTEM_MESSAGE_CONTENT)
                .build();
    }
}
