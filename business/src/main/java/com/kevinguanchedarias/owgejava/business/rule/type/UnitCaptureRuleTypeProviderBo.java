package com.kevinguanchedarias.owgejava.business.rule.type;

import com.kevinguanchedarias.owgejava.dto.rule.RuleExtraArgDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnitCaptureRuleTypeProviderBo implements RuleTypeProvider {
    public static final String PROVIDER_ID = "UNIT_CAPTURE";

    @Override
    public String getRuleTypeId() {
        return PROVIDER_ID;
    }

    @Override
    public RuleTypeDescriptorDto findRuleTypeDescriptor() {
        return RuleTypeDescriptorDto.builder()
                .extraArgs(List.of(
                        RuleExtraArgDto.builder().id(1).name("CRUD.RULES.ARG.UNIT_CAPTURE_PROBABILITY").formType("number").build(),
                        RuleExtraArgDto.builder().id(2).name("CRUD.RULES.ARG.UNIT_CAPTURE_PERCENTAGE").formType("number").build()
                ))
                .build();
    }
}
