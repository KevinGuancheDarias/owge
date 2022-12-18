package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.Hibernate;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UnitDto extends CommonDtoWithImageStore<Integer, Unit> implements DtoWithImprovements {
    private Boolean hasToDisplayInRequirements;
    private Integer points = 0;
    private Integer time = 60;
    private Integer primaryResource = 100;
    private Integer secondaryResource = 100;
    private Integer energy;
    private Integer typeId;
    private String typeName;
    private Integer attack;
    private Integer health = 1;
    private Integer shield;
    private Integer charge;
    private Boolean isUnique = false;
    private Boolean canFastExplore = false;
    private Double speed;
    private ImprovementDto improvement;
    private Boolean clonedImprovements = false;
    private SpeedImpactGroupDto speedImpactGroup;
    private AttackRuleDto attackRule;
    private CriticalAttackDto criticalAttack;
    private Boolean bypassShield = false;
    private Boolean isInvisible = false;
    private Integer storedWeight = 1;
    private Long storageCapacity;

    private List<RequirementInformationDto> requirements;
    private List<InterceptableSpeedGroupDto> interceptableSpeedGroups;

    @Override
    public void dtoFromEntity(Unit entity) {
        super.dtoFromEntity(entity);
        loadData(entity);
        interceptableSpeedGroups = null;
        UnitType typeEntity = entity.getType();
        typeId = typeEntity.getId();
        typeName = typeEntity.getName();
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
        List<InterceptableSpeedGroup> interceptableSpeedGroupsEntity = entity.getInterceptableSpeedGroups();
        if (Hibernate.isInitialized(interceptableSpeedGroupsEntity)
                && !CollectionUtils.isEmpty(interceptableSpeedGroupsEntity)) {
            interceptableSpeedGroups = interceptableSpeedGroupsEntity.stream().map(current -> {
                var dto = new InterceptableSpeedGroupDto();
                dto.dtoFromEntity(current);
                return dto;
            }).collect(Collectors.toList());
        }
        DtoWithImprovements.super.dtoFromEntity(entity);
    }

    private void loadData(Unit entity) {
        hasToDisplayInRequirements = Boolean.TRUE.equals(entity.getHasToDisplayInRequirements());
        points = entity.getPoints();
        time = entity.getTime();
        primaryResource = entity.getPrimaryResource();
        secondaryResource = entity.getSecondaryResource();
        energy = entity.getEnergy();
        attack = entity.getAttack();
        health = entity.getHealth();
        shield = entity.getShield();
        charge = entity.getCharge();
        isUnique = Boolean.TRUE.equals(entity.getIsUnique());
        canFastExplore = Boolean.TRUE.equals(entity.getCanFastExplore());
        speed = entity.getSpeed();
        clonedImprovements = Boolean.TRUE.equals(entity.getClonedImprovements());
        bypassShield = Boolean.TRUE.equals(entity.getBypassShield());
        isInvisible = Boolean.TRUE.equals(entity.getIsInvisible());
        storedWeight = entity.getStoredWeight();
        storageCapacity = entity.getStorageCapacity();
    }
}
