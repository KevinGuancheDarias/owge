package com.kevinguanchedarias.owgejava.util;

import com.kevinguanchedarias.owgejava.dto.ImprovementDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import lombok.experimental.UtilityClass;
import org.hibernate.Hibernate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@UtilityClass
public class ImprovementDtoUtil {
    public static <K> ImprovementDto dtoFromEntity(EntityWithImprovements<K> entity) {
        if (entity.getImprovement() != null &&
                (Hibernate.isInitialized(entity.getImprovement()) || TransactionSynchronizationManager.isActualTransactionActive())) {
            var improvementDto = new ImprovementDto();
            improvementDto.dtoFromEntity(entity.getImprovement());
            return improvementDto;
        } else {
            return null;
        }
    }

    public static <K> ImprovementDto loadImprovementsForWiki(EntityWithImprovements<K> entity) {
        var improvement = dtoFromEntity(entity);
        if (improvement != null) {
            var unitTypesUpgrades = improvement.getUnitTypesUpgrades();
            if (unitTypesUpgrades != null) {
                unitTypesUpgrades.forEach(unitTypeUpgrade -> {
                    var unitType = unitTypeUpgrade.getUnitType();
                    if (unitType != null) {
                        unitTypeUpgrade.setUnitTypeId(unitType.getId());
                        unitTypeUpgrade.setUnitType(null);
                    }
                });
            }
        }
        return improvement;
    }
}
