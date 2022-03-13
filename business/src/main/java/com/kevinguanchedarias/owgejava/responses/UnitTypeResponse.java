package com.kevinguanchedarias.owgejava.responses;

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
