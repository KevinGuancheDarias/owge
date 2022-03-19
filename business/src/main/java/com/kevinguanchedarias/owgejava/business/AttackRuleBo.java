package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.AttackRuleDto;
import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import com.kevinguanchedarias.owgejava.repository.AttackRuleEntryRepository;
import com.kevinguanchedarias.owgejava.repository.AttackRuleRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
public class AttackRuleBo implements BaseBo<Integer, AttackRule, AttackRuleDto> {
    public static final String ATTACK_RULE_CACHE_TAG = "attack_rule";

    @Serial
    private static final long serialVersionUID = -6534004738356678530L;

    @Autowired
    private transient AttackRuleRepository repository;

    @Autowired
    private transient AttackRuleEntryRepository attackRuleEntryRepository;

    @Autowired
    private DtoUtilService dtoUtilService;

    @Autowired
    private transient TaggableCacheManager taggableCacheManager;

    @Override
    public Class<AttackRuleDto> getDtoClass() {
        return AttackRuleDto.class;
    }

    @Override
    public JpaRepository<AttackRule, Integer> getRepository() {
        return repository;
    }

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return ATTACK_RULE_CACHE_TAG;
    }

    @Override
    @Transactional
    public AttackRule save(AttackRuleDto dto) {
        var attackRule = dtoUtilService.entityFromDto(AttackRule.class, dto);
        if (attackRule.getId() != null) {
            attackRuleEntryRepository.deleteByAttackRuleId(attackRule.getId());
        }
        AttackRule saved = save(attackRule);
        List<AttackRuleEntry> entries = new ArrayList<>();
        attackRule.setAttackRuleEntries(entries);
        dto.getEntries().forEach(current -> {
            AttackRuleEntry entity = dtoUtilService.entityFromDto(AttackRuleEntry.class, current);
            entity.setAttackRule(saved);
            entries.add(attackRuleEntryRepository.save(entity));
        });
        return saved;
    }

    @Transactional
    @Override
    public void delete(Integer attackRuleId) {
        delete(findByIdOrDie(attackRuleId));
    }

    @Transactional
    @Override
    public void delete(AttackRule attackRule) {
        attackRuleEntryRepository.deleteAll(attackRule.getAttackRuleEntries());
        BaseBo.super.delete(attackRule);
    }

    /**
     * Discovers the attack rule, looking using recursion
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public AttackRule findAttackRule(UnitType type) {
        if (type.getAttackRule() != null) {
            return type.getAttackRule();
        } else if (type.getParent() != null) {
            return findAttackRule(type.getParent());
        } else {
            return null;
        }
    }

    public boolean canAttack(AttackRule attackRule, ObtainedUnit target) {
        if (attackRule != null && attackRule.getAttackRuleEntries() != null) {
            return attackRule.getAttackRuleEntries().stream()
                    .map(ruleEntry -> isUnitMatchingEntry(ruleEntry, target).or(() -> isUnitTypeMatchingEntry(ruleEntry, target)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .orElse(true);
        }
        return true;
    }

    private Optional<Boolean> isUnitMatchingEntry(AttackRuleEntry ruleEntry, ObtainedUnit target) {
        return ruleEntry.getTarget() == AttackableTargetEnum.UNIT && target.getUnit().getId().equals(ruleEntry.getReferenceId())
                ? Optional.of(ruleEntry.getCanAttack())
                : Optional.empty();
    }

    private Optional<Boolean> isUnitTypeMatchingEntry(AttackRuleEntry ruleEntry, ObtainedUnit target) {
        var unitType = findUnitTypeMatchingRule(ruleEntry, target.getUnit().getType());
        return ruleEntry.getTarget() == AttackableTargetEnum.UNIT_TYPE && unitType != null
                ? Optional.of(ruleEntry.getCanAttack())
                : Optional.empty();
    }

    private UnitType findUnitTypeMatchingRule(AttackRuleEntry ruleEntry, UnitType unitType) {
        if (ruleEntry.getReferenceId().equals(unitType.getId())) {
            return unitType;
        } else if (unitType.getParent() != null) {
            return findUnitTypeMatchingRule(ruleEntry, unitType.getParent());
        } else {
            return null;
        }
    }

}
