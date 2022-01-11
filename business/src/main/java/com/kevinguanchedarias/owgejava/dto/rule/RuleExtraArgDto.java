package com.kevinguanchedarias.owgejava.dto.rule;

import com.kevinguanchedarias.owgejava.dto.base.IdNameDto;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@EqualsAndHashCode(callSuper = true)
@Value
@Jacksonized
@SuperBuilder
public class RuleExtraArgDto extends IdNameDto {
    String formType;
}
