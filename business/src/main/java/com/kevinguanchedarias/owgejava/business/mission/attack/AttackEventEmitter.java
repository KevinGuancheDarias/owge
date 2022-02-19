package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef.AfterAttackEndListener;
import com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef.AfterUnitKilledCalculationListener;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class AttackEventEmitter {
    private final List<AfterUnitKilledCalculationListener> afterUnitKilledCalculationListeners;
    private final List<AfterAttackEndListener> afterAttackEndListeners;

    public void emitAfterUnitKilledCalculation(AttackInformation information, AttackObtainedUnit attacker, AttackObtainedUnit victim, long killed) {
        this.afterUnitKilledCalculationListeners.forEach(listener -> listener.onAfterUnitKilledCalculation(information, attacker, victim, killed));
    }

    public void emitAttackEnd(AttackInformation information) {
        this.afterAttackEndListeners.forEach(listener -> listener.onAttackEnd(information));
    }
}
