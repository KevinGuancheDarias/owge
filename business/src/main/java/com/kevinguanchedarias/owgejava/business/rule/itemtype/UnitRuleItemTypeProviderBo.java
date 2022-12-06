package com.kevinguanchedarias.owgejava.business.rule.itemtype;

import com.kevinguanchedarias.owgejava.dto.base.IdNameDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleItemTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UnitRuleItemTypeProviderBo implements RuleItemTypeProvider {
    public static final String PROVIDER_ID = "UNIT";

    private final UnitRepository unitRepository;

    @Override
    public String getRuleItemTypeId() {
        return PROVIDER_ID;
    }

    @Override
    public RuleItemTypeDescriptorDto findRuleItemTypeDescriptor() {
        return RuleItemTypeDescriptorDto.builder()
                .items(
                        unitRepository.findAll().stream().map(this::unitToIdNameDto).toList()
                )
                .build();
    }

    private IdNameDto unitToIdNameDto(Unit unit) {
        return IdNameDto.builder()
                .id(unit.getId())
                .name(unit.getName())
                .build();
    }
}
