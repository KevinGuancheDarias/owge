package com.kevinguanchedarias.owgejava.dto.wiki;

import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.util.DtoUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UnitTypeWikiDto extends UnitTypeDto {
    private Integer shareMaxCountId;
    private Integer parentId;
    private Integer speedImpactGroupId;
    private Integer attackRuleId;
    private Integer criticalAttackId;

    @Override
    public void dtoFromEntity(UnitType entity) {
        copyBasicProperties(entity);
        defineMissionLimitation(entity);
        shareMaxCountId = DtoUtil.returnIdOrNull(entity.getShareMaxCount());
        parentId = DtoUtil.returnIdOrNull(entity.getParent());
        speedImpactGroupId = DtoUtil.returnIdOrNull(entity.getSpeedImpactGroup());
        attackRuleId = DtoUtil.returnIdOrNull(entity.getAttackRule());
        criticalAttackId = DtoUtil.returnIdOrNull(entity.getCriticalAttack());
    }
}
