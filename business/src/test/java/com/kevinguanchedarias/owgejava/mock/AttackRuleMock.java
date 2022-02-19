package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.dto.AttackRuleDto;
import com.kevinguanchedarias.owgejava.dto.AttackRuleEntryDto;
import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class AttackRuleMock {
    public static final String ATTACK_RULE_NAME = "fooAttack";
    public static final AttackableTargetEnum ATTACK_RULE_TARGET = AttackableTargetEnum.UNIT;
    public static final int ATTACK_RULE_TARGET_REF_ID = 19;

    public static AttackRule givenAttackRule(Integer id) {
        return AttackRule.builder()
                .id(id)
                .name(ATTACK_RULE_NAME)
                .attackRuleEntries(List.of(
                        givenAttackRuleEntry(true),
                        givenAttackRuleEntry(false)
                ))
                .build();
    }

    public static AttackRuleEntry givenAttackRuleEntry(boolean canAttack) {
        return AttackRuleEntry.builder()
                .canAttack(canAttack)
                .target(ATTACK_RULE_TARGET)
                .referenceId(ATTACK_RULE_TARGET_REF_ID)
                .build();
    }

    public static AttackRuleDto givenAttackRuleDto(Integer id) {
        var retVal = new AttackRuleDto();
        retVal.setId(id);
        retVal.setName(ATTACK_RULE_NAME);
        retVal.setEntries(List.of(
                givenAttackRuleEntryDto(true),
                givenAttackRuleEntryDto(false)
        ));
        return retVal;
    }

    public static AttackRuleEntryDto givenAttackRuleEntryDto(boolean canAttack) {
        var retVal = new AttackRuleEntryDto();
        retVal.setCanAttack(canAttack);
        retVal.setTarget(ATTACK_RULE_TARGET);
        retVal.setReferenceId(ATTACK_RULE_TARGET_REF_ID);
        return retVal;
    }
}
