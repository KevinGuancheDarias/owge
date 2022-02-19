package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import com.kevinguanchedarias.owgejava.repository.AttackRuleEntryRepository;
import com.kevinguanchedarias.owgejava.repository.AttackRuleRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.AttackRuleMock.ATTACK_RULE_TARGET_REF_ID;
import static com.kevinguanchedarias.owgejava.mock.AttackRuleMock.givenAttackRule;
import static com.kevinguanchedarias.owgejava.mock.AttackRuleMock.givenAttackRuleDto;
import static com.kevinguanchedarias.owgejava.mock.AttackRuleMock.givenAttackRuleEntry;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = AttackRuleBo.class
)
@MockBean({
        AttackRuleRepository.class,
        AttackRuleEntryRepository.class,
        DtoUtilService.class
})
class AttackRuleBoTest {
    private static final int ATTACK_RULE_ID = 18;

    private final AttackRuleBo attackRuleBo;
    private final AttackRuleRepository attackRuleRepository;
    private final AttackRuleEntryRepository attackRuleEntryRepository;
    private final DtoUtilService dtoUtilService;

    @Autowired
    AttackRuleBoTest(
            AttackRuleBo attackRuleBo,
            AttackRuleRepository attackRuleRepository,
            AttackRuleEntryRepository attackRuleEntryRepository,
            DtoUtilService dtoUtilService
    ) {
        this.attackRuleBo = attackRuleBo;
        this.attackRuleRepository = attackRuleRepository;
        this.attackRuleEntryRepository = attackRuleEntryRepository;
        this.dtoUtilService = dtoUtilService;
    }

    @Test
    void save_should_save_without_deleting_old_if_id_is_null() {
        var attackRuleDto = givenAttackRuleDto(null);
        var attackRule = givenAttackRule(null);
        given(dtoUtilService.entityFromDto(AttackRule.class, attackRuleDto))
                .willReturn(attackRule);
        given(dtoUtilService.entityFromDto(AttackRuleEntry.class, attackRuleDto.getEntries().get(0)))
                .willReturn(attackRule.getAttackRuleEntries().get(0));
        given(dtoUtilService.entityFromDto(AttackRuleEntry.class, attackRuleDto.getEntries().get(1)))
                .willReturn(attackRule.getAttackRuleEntries().get(1));
        given(attackRuleRepository.save(attackRule)).willReturn(attackRule);
        var captor = ArgumentCaptor.forClass(AttackRuleEntry.class);

        attackRuleBo.save(attackRuleDto);

        verify(attackRuleEntryRepository, never()).deleteByAttackRuleId(any());
        verify(dtoUtilService, times(1)).entityFromDto(AttackRule.class, attackRuleDto);
        verify(attackRuleRepository, times(1)).save(attackRule);
        verify(attackRuleEntryRepository, times(2)).save(captor.capture());
        captor.getAllValues()
                .forEach(savedEntry -> assertThat(savedEntry.getAttackRule()).isEqualTo(attackRule));
    }

    @Test
    void save_should_save__deleting_old_if_id_is_NOT_null() {
        var attackRuleDto = givenAttackRuleDto(ATTACK_RULE_ID);
        var attackRule = givenAttackRule(ATTACK_RULE_ID);
        given(dtoUtilService.entityFromDto(AttackRule.class, attackRuleDto))
                .willReturn(attackRule);
        given(dtoUtilService.entityFromDto(AttackRuleEntry.class, attackRuleDto.getEntries().get(0)))
                .willReturn(attackRule.getAttackRuleEntries().get(0));
        given(dtoUtilService.entityFromDto(AttackRuleEntry.class, attackRuleDto.getEntries().get(1)))
                .willReturn(attackRule.getAttackRuleEntries().get(1));
        given(attackRuleRepository.save(attackRule)).willReturn(attackRule);
        var captor = ArgumentCaptor.forClass(AttackRuleEntry.class);

        attackRuleBo.save(attackRuleDto);

        verify(attackRuleEntryRepository, times(1)).deleteByAttackRuleId(any());
        verify(dtoUtilService, times(1)).entityFromDto(AttackRule.class, attackRuleDto);
        verify(attackRuleRepository, times(1)).save(attackRule);
        verify(attackRuleEntryRepository, times(2)).save(captor.capture());
        captor.getAllValues()
                .forEach(savedEntry -> assertThat(savedEntry.getAttackRule()).isEqualTo(attackRule));
    }

    @Test
    void findAttackRule_should_type_rule_if_not_null() {
        var type = givenEntity();
        var attackRule = givenAttackRule(9);
        type.setAttackRule(attackRule);

        assertThat(attackRuleBo.findAttackRule(type)).isEqualTo(attackRule);
    }

    @Test
    void findAttackRule_should_find_parent_attack_rule_if_own_is_null() {
        var type = givenEntity();
        var parentType = givenEntity(1921);
        var attackRule = givenAttackRule(9);
        type.setParent(parentType);
        parentType.setAttackRule(attackRule);

        assertThat(attackRuleBo.findAttackRule(type)).isEqualTo(attackRule);
    }

    @Test
    void findAttackRule_should_return_null_if_root_parent_is_null_and_direct_parent_has_no_rule() {
        var type = givenEntity();
        var parentType = givenEntity(1921);
        type.setParent(parentType);

        assertThat(attackRuleBo.findAttackRule(type)).isNull();
    }

    @Test
    void canAttack_should_return_true_if_attack_rule_is_null() {
        var ou = givenObtainedUnit1();

        assertThat(attackRuleBo.canAttack(null, ou)).isTrue();
    }

    @Test
    void canAttack_should_return_true_if_attack_rule_has_no_entries() {
        var attackRule = givenAttackRule(170);
        attackRule.setAttackRuleEntries(null);
        var ou = givenObtainedUnit1();

        assertThat(attackRuleBo.canAttack(attackRule, ou)).isTrue();
    }

    @Test
    void canAttack_should_return_true_if_no_rules_matches_the_target_unit() {
        var attackRule = givenAttackRule(170);
        var ou = givenObtainedUnit1();

        assertThat(attackRuleBo.canAttack(attackRule, ou)).isTrue();
    }

    @Test
    void canAttack_should_return_true_if_matching_UNIT_rule_value_is_true() {
        var attackRule = givenAttackRule(170);
        var ou = givenObtainedUnit1();
        ou.getUnit().setId(ATTACK_RULE_TARGET_REF_ID);

        assertThat(attackRuleBo.canAttack(attackRule, ou)).isTrue();
    }

    @Test
    void canAttack_should_return_false_if_matching_UNIT_rule_value_is_false() {
        var attackRule = givenAttackRule(170);
        attackRule.setAttackRuleEntries(List.of(givenAttackRuleEntry(false)));
        var ou = givenObtainedUnit1();
        ou.getUnit().setId(ATTACK_RULE_TARGET_REF_ID);

        assertThat(attackRuleBo.canAttack(attackRule, ou)).isFalse();
    }

    @Test
    void canAttack_should_return_true_if_matching_UNIT_TYPE_rule_value_is_true() {
        var attackRule = givenAttackRule(170);
        var matchingEntry = AttackRuleEntry.builder()
                .canAttack(true)
                .target(AttackableTargetEnum.UNIT_TYPE)
                .referenceId(ATTACK_RULE_TARGET_REF_ID)
                .build();
        attackRule.setAttackRuleEntries(List.of(matchingEntry));
        var ou = givenObtainedUnit1();
        ou.getUnit().getType().setId(ATTACK_RULE_TARGET_REF_ID);

        assertThat(attackRuleBo.canAttack(attackRule, ou)).isTrue();
    }

    @Test
    void canAttack_should_return_false_if_matching_UNIT_TYPE_rule_value_is_false() {
        var attackRule = givenAttackRule(170);
        var matchingEntry = AttackRuleEntry.builder()
                .canAttack(false)
                .target(AttackableTargetEnum.UNIT_TYPE)
                .referenceId(ATTACK_RULE_TARGET_REF_ID)
                .build();
        attackRule.setAttackRuleEntries(List.of(matchingEntry));
        var ou = givenObtainedUnit1();
        ou.getUnit().getType().setId(ATTACK_RULE_TARGET_REF_ID);

        assertThat(attackRuleBo.canAttack(attackRule, ou)).isFalse();
    }

    @Test
    void canAttack_should_return_false_if_matching_UNIT_TYPE_recursive_rule_value_is_false() {
        var attackRule = givenAttackRule(170);
        var matchingEntry = AttackRuleEntry.builder()
                .canAttack(false)
                .target(AttackableTargetEnum.UNIT_TYPE)
                .referenceId(ATTACK_RULE_TARGET_REF_ID)
                .build();
        attackRule.setAttackRuleEntries(List.of(matchingEntry));
        var ou = givenObtainedUnit1();
        ou.getUnit().getType().setParent(givenEntity(ATTACK_RULE_TARGET_REF_ID));

        assertThat(attackRuleBo.canAttack(attackRule, ou)).isFalse();
    }

}
