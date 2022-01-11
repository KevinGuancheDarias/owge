package com.kevinguanchedarias.owgejava.business.rule.type;

import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;

public interface RuleTypeProvider {
    String getRuleTypeId();

    RuleTypeDescriptorDto findRuleTypeDescriptor();
}
