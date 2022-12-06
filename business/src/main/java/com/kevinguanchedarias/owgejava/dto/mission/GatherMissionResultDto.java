package com.kevinguanchedarias.owgejava.dto.mission;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GatherMissionResultDto {
    Double primaryResource;
    Double secondaryResource;
}
