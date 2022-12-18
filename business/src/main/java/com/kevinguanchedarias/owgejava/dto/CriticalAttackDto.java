package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.CriticalAttack;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CriticalAttackDto implements DtoFromEntity<CriticalAttack> {
    Integer id;
    String name;
    List<CriticalAttackEntryDto> entries;

    @Override
    public void dtoFromEntity(CriticalAttack entity) {
        id = entity.getId();
        name = entity.getName();
    }
}
