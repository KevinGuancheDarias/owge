package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    /**
     * Shuffles the units list honouring the deterministic RNG flag carried by the {@link AttackInformation}.
     * <p>
     * When deterministic mode is OFF, behaviour is identical to {@link #shuffleUnits(List)}
     * ({@code Collections.shuffle}) and no trace is emitted. When ON, an explicit Fisher-Yates
     * pass driven by the seeded {@link Random} is used (the exact algorithm
     * {@code Collections.shuffle(list, rnd)} runs internally) so each draw can be traced.
     */
    public void shuffleUnits(List<AttackObtainedUnit> units, AttackInformation attackInformation) {
        if (!attackInformation.isDeterministicRng()) {
            Collections.shuffle(units);
            return;
        }
        Random rnd = attackInformation.getDeterministicRandom();
        for (int i = units.size(); i > 1; i--) {
            int j = rnd.nextInt(i);
            attackInformation.traceRng("shuffle", i, null, null, null, j);
            Collections.swap(units, i - 1, j);
        }
    }
}
