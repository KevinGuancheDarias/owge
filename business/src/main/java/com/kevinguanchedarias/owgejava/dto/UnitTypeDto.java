package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.dto.base.DtoWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class UnitTypeDto extends DtoWithMissionLimitation implements DtoFromEntity<UnitType> {

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

    @Getter
    @Setter
    private CriticalAttackDto criticalAttack;

    private Boolean hasToInheritImprovements = false;
    private List<InheritedImprovementUnitType> inheritedImprovementUnitTypes;

    @Override
    public void dtoFromEntity(UnitType entity) {
        EntityPojoConverterUtil.convertFromTo(this, entity);
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getImage() {
        return image;
    }

    public void setImage(Long image) {
        this.image = image;
    }

    /**
     * @return the imageUrl
     * @author Kevin Guanche Darias
     * @since 0.8.1
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * @param imageUrl the imageUrl to set
     * @author Kevin Guanche Darias
     * @since 0.8.1
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Long maxCount) {
        this.maxCount = maxCount;
    }

    /**
     * @return the shareMaxCount
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public UnitTypeDto getShareMaxCount() {
        return shareMaxCount;
    }

    /**
     * @param shareMaxCount the shareMaxCount to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setShareMaxCount(UnitTypeDto shareMaxCount) {
        this.shareMaxCount = shareMaxCount;
    }

    /**
     * Transient property, when defined, represents the maxCount with all user
     * improvements applied
     *
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Long getComputedMaxCount() {
        return computedMaxCount;
    }

    public void setComputedMaxCount(Long computedMaxCount) {
        this.computedMaxCount = computedMaxCount;
    }

    /**
     * @return the parent
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public UnitTypeDto getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setParent(UnitTypeDto parent) {
        this.parent = parent;
    }

    /**
     * Represents the amount built by the user
     *
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Long getUserBuilt() {
        return userBuilt;
    }

    public void setUserBuilt(Long userBuilt) {
        this.userBuilt = userBuilt;
    }

    /**
     * @return the speedImpactGroup
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public SpeedImpactGroupDto getSpeedImpactGroup() {
        return speedImpactGroup;
    }

    /**
     * @param speedImpactGroup the speedImpactGroup to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setSpeedImpactGroup(SpeedImpactGroupDto speedImpactGroup) {
        this.speedImpactGroup = speedImpactGroup;
    }

    /**
     * @return the attackRule
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public AttackRuleDto getAttackRule() {
        return attackRule;
    }

    /**
     * @param attackRule the attackRule to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setAttackRule(AttackRuleDto attackRule) {
        this.attackRule = attackRule;
    }

    /**
     * @return the hasToInheritImprovements
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public Boolean getHasToInheritImprovements() {
        return hasToInheritImprovements;
    }

    /**
     * @param hasToInheritImprovements the hasToInheritImprovements to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setHasToInheritImprovements(Boolean hasToInheritImprovements) {
        this.hasToInheritImprovements = hasToInheritImprovements;
    }

    /**
     * @return the inheritedImprovementUnitTypes
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<InheritedImprovementUnitType> getInheritedImprovementUnitTypes() {
        return inheritedImprovementUnitTypes;
    }

    /**
     * @param inheritedImprovementUnitTypes the inheritedImprovementUnitTypes to set
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    public void setInheritedImprovementUnitTypes(List<InheritedImprovementUnitType> inheritedImprovementUnitTypes) {
        this.inheritedImprovementUnitTypes = inheritedImprovementUnitTypes;
    }

}
