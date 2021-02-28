package com.kevinguanchedarias.owgejava.response;

import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class UnitTypeResponse extends UnitTypeDto {
    boolean used;
}
