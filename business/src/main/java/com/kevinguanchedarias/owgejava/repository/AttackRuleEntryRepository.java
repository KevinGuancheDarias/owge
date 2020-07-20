package com.kevinguanchedarias.owgejava.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface AttackRuleEntryRepository extends JpaRepository<AttackRuleEntry, Integer> {
	List<AttackRuleEntry> findByAttackRule(AttackRule attackRule);

	void deleteByAttackRuleId(Integer attackRuleId);
}
