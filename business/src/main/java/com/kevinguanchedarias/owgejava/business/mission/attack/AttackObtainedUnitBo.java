package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class AttackObtainedUnitBo {
    private final ImprovementBo improvementBo;

    public AttackObtainedUnit create(ObtainedUnit obtainedUnit, AttackUserInformation attackUserInformation) {
        var unit = obtainedUnit.getUnit();
        var unitType = unit.getType();
        var initialCount = obtainedUnit.getCount();
        double totalAttack = initialCount.doubleValue() * unit.getAttack();
        var userImprovement = attackUserInformation.getUserImprovement();
        totalAttack += (totalAttack * improvementBo.findAsRational(
                (double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.ATTACK, unitType)));
        var totalShield = initialCount.doubleValue() * ObjectUtils.firstNonNull(unit.getShield(), 0);
        totalShield += (totalShield * improvementBo.findAsRational(
                (double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.SHIELD, unitType)));
        var totalHealth = initialCount.doubleValue() * unit.getHealth();
        totalHealth += (totalHealth * improvementBo.findAsRational(
                (double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.DEFENSE, unitType)));
        return AttackObtainedUnit.builder()
                .obtainedUnit(obtainedUnit)
                .user(attackUserInformation)
                .initialCount(obtainedUnit.getCount())
                .finalCount(obtainedUnit.getCount())
                .pendingAttack(totalAttack)
                .totalShield(totalShield)
                .availableShield(totalShield)
                .totalHealth(totalHealth)
                .availableHealth(totalHealth)
                .build();
    }

    public void shuffleUnits(List<AttackObtainedUnit> units) {
        Collections.shuffle(units);
    }
}
