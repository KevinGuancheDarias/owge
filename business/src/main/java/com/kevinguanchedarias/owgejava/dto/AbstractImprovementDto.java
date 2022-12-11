/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Data
public abstract class AbstractImprovementDto implements DtoFromEntity<Improvement> {
    private Float morePrimaryResourceProduction;
    private Float moreSecondaryResourceProduction;
    private Float moreEnergyProduction;
    private Float moreChargeCapacity;
    private Float moreMissions;
    private Float moreUpgradeResearchSpeed;
    private Float moreUnitBuildSpeed;
    private List<ImprovementUnitTypeDto> unitTypesUpgrades;

    @Override
    public void dtoFromEntity(Improvement entity) {
        morePrimaryResourceProduction = entity.getMorePrimaryResourceProduction();
        moreSecondaryResourceProduction = entity.getMoreSecondaryResourceProduction();
        moreEnergyProduction = entity.getMoreEnergyProduction();
        moreChargeCapacity = entity.getMoreChargeCapacity();
        moreMissions = entity.getMoreMisions();
        moreUpgradeResearchSpeed = entity.getMoreUpgradeResearchSpeed();
        moreUnitBuildSpeed = entity.getMoreUnitBuildSpeed();
        loadUnitTypes(entity);
    }

    public AbstractImprovementDto addMorePrimaryResourceProduction(Float value) {
        morePrimaryResourceProduction += value;
        return this;
    }

    public AbstractImprovementDto addMoreSecondaryResourceProduction(Float value) {
        moreSecondaryResourceProduction += value;
        return this;
    }

    public AbstractImprovementDto addMoreEnergyProduction(Float value) {
        moreEnergyProduction += value;
        return this;
    }

    public AbstractImprovementDto addMoreChargeCapacity(Float value) {
        moreChargeCapacity += value;
        return this;
    }

    public AbstractImprovementDto addMoreMissions(Float value) {
        moreMissions += value;
        return this;
    }

    public AbstractImprovementDto addMoreUpgradeResearchSpeed(Float value) {
        moreUpgradeResearchSpeed += value;
        return this;
    }

    public AbstractImprovementDto addMoreUnitBuildSpeed(Float value) {
        moreUnitBuildSpeed += value;
        return this;
    }

    /**
     * Initializes all the values to zero
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    protected void initToZeroes() {
        morePrimaryResourceProduction = 0F;
        moreSecondaryResourceProduction = 0F;
        moreEnergyProduction = 0F;
        moreChargeCapacity = 0F;
        moreMissions = 0F;
        moreUpgradeResearchSpeed = 0F;
        moreUnitBuildSpeed = 0F;
        unitTypesUpgrades = new ArrayList<>();
    }

    private void loadUnitTypes(Improvement entity) {
        unitTypesUpgrades = List.of();
        if (entity.getUnitTypesUpgrades() != null) {
            unitTypesUpgrades = entity.getUnitTypesUpgrades()
                    .stream()
                    .map(current -> {
                        var currentDto = new ImprovementUnitTypeDto();
                        currentDto.dtoFromEntity(current);
                        return currentDto;
                    }).toList();
        }
    }
}
