package com.kevinguanchedarias.owgejava.responses;

import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class CriticalAttackInformationResponse {
    AttackableTargetEnum target;
    Number targetId;
    String targetName;
    float value;
}
