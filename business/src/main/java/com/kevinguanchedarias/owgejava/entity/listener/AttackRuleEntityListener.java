package com.kevinguanchedarias.owgejava.entity.listener;

import jakarta.persistence.PostLoad;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.repository.AttackRuleEntryRepository;

/**
 * Does a workaround loading rule entries, as doesn't work for unknown reasons
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Component

public class AttackRuleEntityListener {

    private AttackRuleEntryRepository attackRuleEntryRepository;

    @Lazy
    public AttackRuleEntityListener(AttackRuleEntryRepository attackRuleEntryRepository) {
        this.attackRuleEntryRepository = attackRuleEntryRepository;
    }

    /**
     * Apply the workaround to the collection loading
     *
     * @param attackRule
     * @todo Complete issue <a href=
     * "https://github.com/KevinGuancheDarias/owge/issues/258">#258</a>
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostLoad
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadRuleEntries(AttackRule attackRule) {
        attackRule.setAttackRuleEntries(attackRuleEntryRepository.findByAttackRule(attackRule));
    }
}
