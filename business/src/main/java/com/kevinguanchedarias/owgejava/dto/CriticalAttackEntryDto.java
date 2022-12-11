package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.CriticalAttackEntry;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import lombok.*;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CriticalAttackEntryDto implements DtoFromEntity<CriticalAttackEntry> {
    private Integer id;
    private AttackableTargetEnum target;
    private Integer referenceId;
    private String referenceName;
    private Float value;

    @Override
    public void dtoFromEntity(CriticalAttackEntry entity) {
        id = entity.getId();
        target = entity.getTarget();
        referenceId = entity.getReferenceId();
        value = entity.getValue();
    }
}
