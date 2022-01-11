package com.kevinguanchedarias.owgejava.dto.rule;

import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class RuleDto {
    int id;
    String type;
    String originType;
    Long originId;
    String destinationType;
    Long destinationId;
    List<Object> extraArgs;
    List<RequirementInformationDto> requirements;
}
