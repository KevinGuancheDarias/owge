package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.CriticalAttack;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CriticalAttackDto implements WithDtoFromEntityTrait<CriticalAttack> {
    Integer id;
    String name;
    List<CriticalAttackEntryDto> entries;
}
