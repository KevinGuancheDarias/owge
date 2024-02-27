package com.kevinguanchedarias.owgejava.dto.wiki;

import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.util.DtoUtil;
import com.kevinguanchedarias.owgejava.util.ImprovementDtoUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Data
public class UnitWikiDto extends UnitDto {
    private Integer speedImpactGroupId;
    private Integer attackRuleId;
    private Integer criticalAttackId;

    @Override
    public void dtoFromEntity(Unit entity) {
        handleCommon(entity);
        handleImageLoad(entity);
        loadData(entity);
        setImprovement(ImprovementDtoUtil.loadImprovementsForWiki(entity));
        speedImpactGroupId = DtoUtil.returnIdOrNull(entity.getSpeedImpactGroup());
        attackRuleId = DtoUtil.returnIdOrNull(entity.getAttackRule());
        criticalAttackId = DtoUtil.returnIdOrNull(entity.getCriticalAttack());
    }
}
