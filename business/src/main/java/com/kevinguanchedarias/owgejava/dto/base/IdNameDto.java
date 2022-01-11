package com.kevinguanchedarias.owgejava.dto.base;

import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@NonFinal
@SuperBuilder
@Jacksonized
public class IdNameDto {
    Number id;
    String name;
}
