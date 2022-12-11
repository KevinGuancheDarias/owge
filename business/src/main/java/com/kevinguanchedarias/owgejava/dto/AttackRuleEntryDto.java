package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import lombok.Data;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
public class AttackRuleEntryDto implements DtoFromEntity<AttackRuleEntry> {
    private Integer id;
    private AttackableTargetEnum target;
    private Integer referenceId;
    private String referenceName;
    private Boolean canAttack = false;

    @Override
    public void dtoFromEntity(AttackRuleEntry entity) {
        id = entity.getId();
        target = entity.getTarget();
        referenceId = entity.getReferenceId();
        referenceName = entity.getReferenceName();
        canAttack = entity.getCanAttack();
    }
}
