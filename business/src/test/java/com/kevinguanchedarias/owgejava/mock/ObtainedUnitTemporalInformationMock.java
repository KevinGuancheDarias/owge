package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.jdbc.ObtainedUnitTemporalInformation;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@UtilityClass
public class ObtainedUnitTemporalInformationMock {
    public static final long OBTAINED_UNIT_TEMPORAL_INFORMATION_ID = 61923;
    public static final long DURATION = 180;
    public static final Instant EXPIRATION = LocalDateTime.parse("1993-03-12T11:12:14").toInstant(ZoneOffset.UTC);

    public static ObtainedUnitTemporalInformation givenObtainedUnitTemporalInformation() {
        return ObtainedUnitTemporalInformation.builder()
                .id(OBTAINED_UNIT_TEMPORAL_INFORMATION_ID)
                .duration(DURATION)
                .expiration(EXPIRATION)
                .build();
    }
}
