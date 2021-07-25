/**
 *
 */
package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.dto.AbstractImprovementDto;
import com.kevinguanchedarias.owgejava.dto.ImprovementDto;
import com.kevinguanchedarias.owgejava.dto.ImprovementUnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * Represents the full sum of a improvement
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class GroupedImprovement extends AbstractImprovementDto {

    /**
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public GroupedImprovement() {
        initToZeroes();
    }

    /**
     * Adds the list of improvements to the group
     *
     * @param improvements
     * @return
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public GroupedImprovement add(List<ImprovementDto> improvements) {
        improvements.forEach(this::doAdd);
        return this;
    }

    /**
     * Adds one entire group to this group
     *
     * @param groupedImprovement
     * @return
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public GroupedImprovement add(GroupedImprovement groupedImprovement) {
        doAdd(groupedImprovement);
        return this;
    }

    /**
     * Adds one improvement entity to this group
     *
     * @param improvement
     * @return
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public GroupedImprovement add(Improvement improvement) {
        if (improvement != null) {
            ImprovementDto improvementDto = new ImprovementDto();
            improvementDto.dtoFromEntity(improvement);
            doAdd(improvementDto);
        }
        return this;
    }

    /**
     * Finds the value of a unit type improvement (sum of all even of different
     * {@link UnitType}
     *
     * @deprecated This method is not used, may never ever be helpful
     * @param improvementTypeEnum
     * @return
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Deprecated(since = "0.9.0")
    public Long findUnitTypeImprovement(ImprovementTypeEnum improvementTypeEnum) {
        return getUnitTypesUpgrades().stream().filter(current -> improvementTypeEnum.name().equals(current.getType()))
                .map(ImprovementUnitTypeDto::getValue).reduce(0L, (sum, current) -> sum + current);
    }

    /**
     * Finds the value of a unit type improvement for a given unit type
     *
     * @param improvementTypeEnum Type of improvement
     * @param unitTypeId          Target {@link UnitType} id
     * @return
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Long findUnitTypeImprovement(ImprovementTypeEnum improvementTypeEnum, UnitType unitType) {
        if (unitType != null) {
            Long retVal = getUnitTypesUpgrades().stream()
                    .filter(current -> improvementTypeEnum.name().equals(current.getType())
                            && unitType.getId().equals(current.getUnitType().getId()))
                    .map(ImprovementUnitTypeDto::getValue).reduce(0L, (sum, current) -> sum + current);
            if (Boolean.TRUE.equals(unitType.getHasToInheritImprovements())) {
                var parent = unitType.getParent();
                if (improvementTypeEnum == ImprovementTypeEnum.AMOUNT) {
                    var shareMaxCount = unitType.getShareMaxCount();
                    if (shareMaxCount != null && shareMaxCount.getShareMaxCount() != null && !shareMaxCount.getShareMaxCount().equals(shareMaxCount)) {
                        retVal += findUnitTypeImprovement(improvementTypeEnum, shareMaxCount);
                    }
                } else if (parent != null) {
                    retVal += findUnitTypeImprovement(improvementTypeEnum, parent);
                }
            }
            return retVal;
        } else {
            return 0L;
        }
    }

    private void doAdd(AbstractImprovementDto improvementDto) {
        if (improvementDto != null) {
            addMoreChargeCapacity(safeSum(improvementDto.getMoreChargeCapacity()));
            addMoreEnergyProduction(safeSum(improvementDto.getMoreEnergyProduction()));
            addMoreMissions(safeSum(improvementDto.getMoreMisions()));
            addMorePrimaryResourceProduction(safeSum(improvementDto.getMorePrimaryResourceProduction()));
            addMoreSecondaryResourceProduction(safeSum(improvementDto.getMoreSecondaryResourceProduction()));
            addMoreUnitBuildSpeed(safeSum(improvementDto.getMoreUnitBuildSpeed()));
            addMoreUpgradeResearchSpeed(safeSum(improvementDto.getMoreUpgradeResearchSpeed()));
            if (improvementDto.getUnitTypesUpgrades() != null) {
                improvementDto.getUnitTypesUpgrades().forEach(this::addToType);
            }
        }
    }

    private Float safeSum(Float numericValue) {
        return ObjectUtils.firstNonNull(numericValue, 0F);
    }

    private void addToType(ImprovementUnitTypeDto improvement) {
        ImprovementUnitTypeDto existing = getUnitTypesUpgrades().stream()
                .filter(current -> current.getType().equals(improvement.getType())
                        && current.getUnitType().getId().equals(improvement.getUnitType().getId()))
                .findFirst().orElse(null);
        if (existing == null) {
            ImprovementUnitTypeDto cloned = new ImprovementUnitTypeDto();
            BeanUtils.copyProperties(improvement, cloned);
            getUnitTypesUpgrades().add(cloned);
        } else {
            existing.setValue(existing.getValue() + improvement.getValue());
        }

    }
}
