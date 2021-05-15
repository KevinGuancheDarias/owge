package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.CriticalAttackDto;
import com.kevinguanchedarias.owgejava.dto.CriticalAttackEntryDto;
import com.kevinguanchedarias.owgejava.entity.CriticalAttack;
import com.kevinguanchedarias.owgejava.entity.CriticalAttackEntry;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import com.kevinguanchedarias.owgejava.repository.CriticalAttackEntryRepository;
import com.kevinguanchedarias.owgejava.repository.CriticalAttackRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.responses.CriticalAttackInformationResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CriticalAttackBo implements BaseReadBo<Integer, CriticalAttack> {

    private static final float DEFAULT_VALUE_WHEN_MISSING = 1F;

    private final CriticalAttackRepository repository;
    private final CriticalAttackEntryRepository entriesRepository;
    private final UnitTypeBo unitTypeBo;
    private final UnitRepository unitRepository;

    @Override
    public JpaRepository<CriticalAttack, Integer> getRepository() {
        return repository;
    }

    @Transactional
    public CriticalAttack save(CriticalAttackDto dto) {
        var entity = dtoToEntity(dto);
        if (entity.getId() != null) {
            entriesRepository.deleteByCriticalAttackId(entity.getId());
        }
        var saved = repository.save(entity);
        List<CriticalAttackEntry> entries = new ArrayList<>();
        saved.setEntries(entries);
        dto.getEntries().forEach(entryDto -> {
            var entryEntity = entryDtoToEntity(entryDto);
            if(entryEntity.getValue() < 0) {
                entryEntity.setValue(Math.abs(entryEntity.getValue()));
            }
            entryEntity.setCriticalAttack(entity);
            entries.add(entriesRepository.save(entryEntity));
        });
        return saved;
    }

    @Transactional
    public void delete(Integer criticalId) {
        delete(findByIdOrDie(criticalId));
    }

    @Transactional
    public void delete(CriticalAttack critical) {
        entriesRepository.deleteAll(critical.getEntries());
        repository.delete(critical);
    }

    public CriticalAttackDto toDto(CriticalAttack criticalAttack) {
        return CriticalAttackDto.builder()
                .id(criticalAttack.getId())
                .name(criticalAttack.getName())
                .entries(criticalAttack.getEntries().stream()
                        .map(entry -> CriticalAttackEntryDto.builder()
                                .id(entry.getId())
                                .referenceId(entry.getReferenceId())
                                .target(entry.getTarget())
                                .value(entry.getValue())
                                .build()
                        ).collect(Collectors.toList())
                )
                .build();
    }

    public CriticalAttackEntry findApplicableCriticalEntry(CriticalAttack criticalAttack, Unit unit) {
        if (criticalAttack != null) {
            for (CriticalAttackEntry ruleEntry : criticalAttack.getEntries()) {
                if (
                        (ruleEntry.getTarget() == AttackableTargetEnum.UNIT && unit.getId().equals(ruleEntry.getReferenceId()))
                                ||
                                (ruleEntry.getTarget() == AttackableTargetEnum.UNIT_TYPE
                                        && findUnitTypeMatchingRule(ruleEntry, unit.getType()) != null)) {
                    return ruleEntry;
                }
            }
        }
        return null;
    }

    public CriticalAttack findUsedCriticalAttack(UnitType type) {
        if (type.getCriticalAttack() != null) {
            return type.getCriticalAttack();
        } else if (type.getParent() != null) {
            return findUsedCriticalAttack(type.getParent());
        } else {
            return null;
        }
    }

    public List<CriticalAttackInformationResponse> buildFullInformation(CriticalAttack criticalAttack) {
        List<CriticalAttackInformationResponse> retVal = new ArrayList<>();
        var entries = criticalAttack.getEntries();
        entries.stream().filter(entry -> entry.getTarget() == AttackableTargetEnum.UNIT)
                .map(this::mapEntryToInformationResponse).forEach(retVal::add);

        unitTypeBo.findAll().stream().forEach(type ->
                entries.stream().filter(entry -> findUnitTypeMatchingRule(entry, type) != null).findFirst()
                        .ifPresentOrElse(
                                entry -> retVal.add(mapEntryToInformationResponse(entry)),
                                () -> retVal.add(createDefaultInformationResponse(type))
                        )
        );
        retVal.sort((a, b) -> (int) (b.getValue() * 1000 - a.getValue() * 1000));
        return retVal;

    }

    private CriticalAttackInformationResponse createDefaultInformationResponse(UnitType unitType) {
        return CriticalAttackInformationResponse.builder()
                .value(DEFAULT_VALUE_WHEN_MISSING)
                .target(AttackableTargetEnum.UNIT_TYPE)
                .targetId(unitType.getId())
                .targetName(unitType.getName())
                .build();
    }

    private CriticalAttackInformationResponse mapEntryToInformationResponse(CriticalAttackEntry entry) {
        var refId = entry.getReferenceId();
        return CriticalAttackInformationResponse.builder()
                .target(entry.getTarget())
                .targetId(refId)
                .targetName(entry.getTarget() == AttackableTargetEnum.UNIT
                        ? unitRepository.findById(refId).orElseThrow().getName()
                        : unitTypeBo.findById(refId).getName()
                )
                .value(entry.getValue())
                .build();
    }

    private UnitType findUnitTypeMatchingRule(CriticalAttackEntry ruleEntry, UnitType unitType) {
        if (ruleEntry.getReferenceId().equals(unitType.getId())) {
            return unitType;
        } else if (unitType.getParent() != null) {
            return findUnitTypeMatchingRule(ruleEntry, unitType.getParent());
        } else {
            return null;
        }
    }

    private CriticalAttack dtoToEntity(CriticalAttackDto dto) {
        return CriticalAttack.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }

    private CriticalAttackEntry entryDtoToEntity(CriticalAttackEntryDto dto) {
        return CriticalAttackEntry.builder()
                .id(dto.getId())
                .target(dto.getTarget())
                .referenceId(dto.getReferenceId())
                .value(dto.getValue())
                .build();
    }

}
