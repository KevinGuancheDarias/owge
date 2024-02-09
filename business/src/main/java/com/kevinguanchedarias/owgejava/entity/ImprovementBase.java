package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.io.Serial;

@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class ImprovementBase<K> implements EntityWithId<K> {
    @Serial
    private static final long serialVersionUID = 8483043984040996933L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private K id;

    @Column(name = "more_primary_resource_production")
    private Float morePrimaryResourceProduction = 0.0F;

    @Column(name = "more_secondary_resource_production")
    private Float moreSecondaryResourceProduction = 0.0F;

    @Column(name = "more_energy_production")
    private Float moreEnergyProduction = 0.0F;

    @Column(name = "more_charge_capacity")
    private Float moreChargeCapacity = 0.0F;

    @Column(name = "more_missions_value")
    private Float moreMisions = 0.0F;

    @Column(name = "more_upgrade_research_speed")
    private Float moreUpgradeResearchSpeed = 0F;

    @Column(name = "more_unit_build_speed")
    private Float moreUnitBuildSpeed = 0F;

    public ImprovementBase<K> addMorePrimaryResourceProduction(Float value) {
        morePrimaryResourceProduction += value;
        return this;
    }

    public ImprovementBase<K> addMoreSecondaryResourceProduction(Float value) {
        moreSecondaryResourceProduction += value;
        return this;
    }

    public ImprovementBase<K> addMoreEnergyProduction(Float value) {
        moreEnergyProduction += value;
        return this;
    }

    public ImprovementBase<K> addMoreChargeCapacity(Float value) {
        moreChargeCapacity += value;
        return this;
    }

    public ImprovementBase<K> addMoreMissions(Float value) {
        moreMisions += value;
        return this;
    }

    public ImprovementBase<K> addMoreUpgradeResearchSpeed(Float value) {
        moreUpgradeResearchSpeed += value;
        return this;
    }

    public ImprovementBase<K> addMoreUnitBuildSpeed(Float value) {
        moreUnitBuildSpeed += value;
        return this;
    }

}
