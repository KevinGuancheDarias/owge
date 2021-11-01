package com.kevinguanchedarias.owgejava.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Value
@EqualsAndHashCode
public class FactionSpawnLocationDto {
    Integer galaxyId;
    Long sectorRangeStart;
    Long sectorRangeEnd;
    Long quadrantRangeStart;
    Long quadrantRangeEnd;
}
