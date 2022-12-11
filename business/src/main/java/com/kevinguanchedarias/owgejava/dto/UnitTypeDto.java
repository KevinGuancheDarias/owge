package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.dto.base.DtoWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class UnitTypeDto extends DtoWithMissionLimitation implements DtoFromEntity<UnitType> {
    @EqualsAndHashCode.Include
    private Integer id;

    private String name;
    private Long image;
    private String imageUrl;
    private Long maxCount;
    private UnitTypeDto shareMaxCount;
    private Long computedMaxCount;
    private UnitTypeDto parent;
    private Long userBuilt;
    private SpeedImpactGroupDto speedImpactGroup;
    private AttackRuleDto attackRule;

    private CriticalAttackDto criticalAttack;

    private Boolean hasToInheritImprovements = false;
    private List<InheritedImprovementUnitType> inheritedImprovementUnitTypes;

    @Override
    public void dtoFromEntity(UnitType entity) {
        copyBasicProperties(entity);
        if (entity.getImage() != null) {
            image = entity.getImage().getId();
            imageUrl = entity.getImage().getUrl();
        }
        if (entity.getShareMaxCount() != null) {
            shareMaxCount = new UnitTypeDto();
            shareMaxCount.dtoFromEntity(entity.getShareMaxCount());
        }
        if (entity.getParent() != null) {
            parent = new UnitTypeDto();
            parent.dtoFromEntity(entity.getParent());
        }
        if (entity.getSpeedImpactGroup() != null) {
            speedImpactGroup = new SpeedImpactGroupDto();
            speedImpactGroup.dtoFromEntity(entity.getSpeedImpactGroup());
        }
        if (entity.getAttackRule() != null) {
            attackRule = new AttackRuleDto();
            attackRule.dtoFromEntity(entity.getAttackRule());
        }
        var criticalAttackEntity = entity.getCriticalAttack();
        if (criticalAttackEntity != null) {
            criticalAttack = new CriticalAttackDto();
            criticalAttack.dtoFromEntity(criticalAttackEntity);
            criticalAttack.setEntries(DtoUtilService.staticDtosFromEntities(CriticalAttackEntryDto.class, criticalAttackEntity.getEntries()));
        }
    }

    private void copyBasicProperties(UnitType unitType) {
        id = unitType.getId();
        name = unitType.getName();
        maxCount = unitType.getMaxCount();
        hasToInheritImprovements = unitType.getHasToInheritImprovements();
    }
}
