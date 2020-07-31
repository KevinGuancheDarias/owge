package com.kevinguanchedarias.owgejava.business;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.AttackRuleDto;
import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.repository.AttackRuleEntryRepository;
import com.kevinguanchedarias.owgejava.repository.AttackRuleRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
public class AttackRuleBo implements BaseBo<Integer, AttackRule, AttackRuleDto> {
	private static final long serialVersionUID = -6534004738356678530L;

	@Autowired
	private transient AttackRuleRepository repository;

	@Autowired
	private transient AttackRuleEntryRepository attackRuleEntryRepository;

	@Autowired
	private DtoUtilService dtoUtilService;

	@Override
	public Class<AttackRuleDto> getDtoClass() {
		return AttackRuleDto.class;
	}

	@Override
	public JpaRepository<AttackRule, Integer> getRepository() {
		return repository;
	}

	@Override
	@Transactional
	public AttackRule save(AttackRuleDto dto) {
		AttackRule attackRule = dtoUtilService.entityFromDto(AttackRule.class, dto);
		if (attackRule.getId() != null) {
			attackRuleEntryRepository.deleteByAttackRuleId(attackRule.getId());
		}
		AttackRule saved = save(attackRule);
		attackRule.setAttackRuleEntries(new ArrayList<>());
		List<AttackRuleEntry> entries = attackRule.getAttackRuleEntries();
		dto.getEntries().forEach(current -> {
			AttackRuleEntry entity = dtoUtilService.entityFromDto(AttackRuleEntry.class, current);
			entity.setAttackRule(saved);
			entries.add(attackRuleEntryRepository.save(entity));
		});
		return saved;
	}
}
