package com.kevinguanchedarias.owgejava.business.mission.attack.listenerdef;

import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;

public interface AfterAttackEndListener {
    void onAttackEnd(AttackInformation information);
}
