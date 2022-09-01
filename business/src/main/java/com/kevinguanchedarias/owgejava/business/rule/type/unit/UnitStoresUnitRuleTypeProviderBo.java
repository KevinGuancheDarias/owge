package com.kevinguanchedarias.owgejava.business.rule.type.unit;

import com.kevinguanchedarias.owgejava.business.rule.type.RuleTypeProvider;
import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;

public class UnitStoresUnitRuleTypeProviderBo implements RuleTypeProvider {
    public static final String PROVIDER_ID = "UNIT_STORES_UNIT";

    @Override
    public String getRuleTypeId() {
        return PROVIDER_ID;
    }

    @Override
    public RuleTypeDescriptorDto findRuleTypeDescriptor() {
        return RuleTypeDescriptorDto.builder().build();
    }
}
