package com.kevinguanchedarias.owgejava;

import lombok.experimental.UtilityClass;
import org.springframework.core.Ordered;

@UtilityClass
public class GlobalConstants {
    public static final String MAX_PLANETS_MESSAGE = "I18N_MAX_PLANETS_EXCEEDED";
    public static final int OPEN_WEBSOCKET_SYNC_FILTER = Ordered.HIGHEST_PRECEDENCE;
    public static final int WEBSOCKET_SYNC_RATE_LIMIT_FILTER = Ordered.HIGHEST_PRECEDENCE;
}
