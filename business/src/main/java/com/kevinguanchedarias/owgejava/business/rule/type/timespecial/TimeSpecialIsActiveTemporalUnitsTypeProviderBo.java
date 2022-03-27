package com.kevinguanchedarias.owgejava.business.rule.type.timespecial;

import com.kevinguanchedarias.owgejava.business.rule.type.RuleTypeProvider;
import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;
import org.springframework.stereotype.Service;

@Service
public class TimeSpecialIsActiveTemporalUnitsTypeProviderBo implements RuleTypeProvider {
    public static final String TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID = "TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS";

    @Override
    public String getRuleTypeId() {
        return TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID;
    }

    @Override
    public RuleTypeDescriptorDto findRuleTypeDescriptor() {
        return RuleTypeDescriptorDto.builder()
                .build();
    }
}

