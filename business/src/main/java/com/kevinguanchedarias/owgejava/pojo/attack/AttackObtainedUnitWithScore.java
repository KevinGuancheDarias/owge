package com.kevinguanchedarias.owgejava.pojo.attack;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class AttackObtainedUnitWithScore {
    AttackObtainedUnit attackObtainedUnit;
    float score;
}
