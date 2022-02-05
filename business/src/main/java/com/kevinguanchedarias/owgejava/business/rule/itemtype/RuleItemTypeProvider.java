package com.kevinguanchedarias.owgejava.business.rule.itemtype;

import com.kevinguanchedarias.owgejava.dto.rule.RuleItemTypeDescriptorDto;

public interface RuleItemTypeProvider {
    String getRuleItemTypeId();

    RuleItemTypeDescriptorDto findRuleItemTypeDescriptor();
}
