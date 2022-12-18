package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ImprovementUnitTypeMock.givenImprovementUnitType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImprovementMock {
    public static final float GROUPED_IMPROVEMENT_MORE_MISSIONS = 3;

    public static final float MORE_CHARGE_CAPACITY = 20;
    public static final float MORE_ENERGY = 70;
    public static final float MORE_MISSIONS = 14;
    public static final float MORE_PR = 40;
    public static final float MORE_SR = 81;
    public static final float MORE_UNIT_BUILD_SPEED = 30;
    public static final float MORE_UPGRADE_RESEARCH_SPEED = 10;

    public static Improvement givenImprovement() {
        return Improvement.builder()
                .id(1)
                .moreChargeCapacity(MORE_CHARGE_CAPACITY)
                .moreEnergyProduction(MORE_ENERGY)
                .moreMisions(MORE_MISSIONS)
                .morePrimaryResourceProduction(MORE_PR)
                .moreSecondaryResourceProduction(MORE_SR)
                .moreUnitBuildSpeed(MORE_UNIT_BUILD_SPEED)
                .moreUpgradeResearchSpeed(MORE_UPGRADE_RESEARCH_SPEED)
                .unitTypesUpgrades(List.of(givenImprovementUnitType(ImprovementTypeEnum.ATTACK)))
                .build();
    }

    public static GroupedImprovement givenUserImprovement() {
        var instance = new GroupedImprovement();
        instance.setMoreMissions(GROUPED_IMPROVEMENT_MORE_MISSIONS);
        return instance;
    }
}
