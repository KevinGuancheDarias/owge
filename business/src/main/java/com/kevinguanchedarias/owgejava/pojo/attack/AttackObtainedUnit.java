package com.kevinguanchedarias.owgejava.pojo.attack;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor
public class AttackObtainedUnit {

    @EqualsAndHashCode.Include
    @ToString.Exclude
    private AttackUserInformation user;

    @EqualsAndHashCode.Include
    private ObtainedUnit obtainedUnit;

    private Long initialCount;

    private Double pendingAttack;
    private boolean noAttack;
    private Double availableShield;
    private Double availableHealth;
    private Long finalCount;

    private Double totalShield;
    private Double totalHealth;

}
