package com.kevinguanchedarias.owgejava.dto.rule;

import com.kevinguanchedarias.owgejava.dto.base.IdNameDto;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Jacksonized
@Builder
public class RuleItemTypeDescriptorDto {
    List<IdNameDto> items;
}
