package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.CriticalAttackEntry;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CriticalAttackEntryDto implements WithDtoFromEntityTrait<CriticalAttackEntry> {
    private Integer id;
    private AttackableTargetEnum target;
    private Integer referenceId;
    private String referenceName;
    private Float value;
}
