package com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef;

import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;

public interface AfterUnitKilledCalculationListener {
    void onAfterUnitKilledCalculation(AttackInformation information, AttackObtainedUnit attacker, AttackObtainedUnit victim, long killed);
}
