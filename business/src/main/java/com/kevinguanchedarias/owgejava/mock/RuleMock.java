package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.dto.base.IdNameDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleItemTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.entity.Rule;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class RuleMock {
    public static final int RULE_ID = 8;
    public static final String TYPE = "fooType";
    public static final String ORIGIN_TYPE = "originType";
    public static final long ORIGIN_ID = 710;
    public static final String DESTINATION_TYPE = "originType";
    public static final long DESTINATION_ID = 7110;
    public static final String FIRST_EXTRA_ARG = "fooArg";
    public static final String SECOND_EXTRA_ARG = "barArg";
    public static final int RULE_ITEM_ID = 4;
    public static final String RULE_ITEM_NAME = "FooItem";

    public static Rule givenRule() {
        return Rule.builder()
                .id(RULE_ID)
                .type(TYPE)
                .originType(ORIGIN_TYPE)
                .originId(ORIGIN_ID)
                .destinationType(DESTINATION_TYPE)
                .destinationId(DESTINATION_ID)
                .extraArgs(FIRST_EXTRA_ARG + "#" + SECOND_EXTRA_ARG)
                .build();
    }

    public static RuleDto givenRuleDto() {
        return RuleDto.builder()
                .id(RULE_ID)
                .type(TYPE)
                .originType(ORIGIN_TYPE)
                .originId(ORIGIN_ID)
                .destinationType(DESTINATION_TYPE)
                .destinationId(DESTINATION_ID)
                .extraArgs(List.of(FIRST_EXTRA_ARG, SECOND_EXTRA_ARG))
                .build();
    }

    public static RuleItemTypeDescriptorDto givenRuleItemTypeDescriptor() {
        return RuleItemTypeDescriptorDto.builder()
                .items(List.of(givenRuleItemIdNameDto()))
                .build();
    }

    public static IdNameDto givenRuleItemIdNameDto() {
        return IdNameDto.builder()
                .id(RULE_ITEM_ID)
                .name(RULE_ITEM_NAME)
                .build();
    }

    public static RuleTypeDescriptorDto givenRuleTypeDescriptorDto() {
        return RuleTypeDescriptorDto
                .builder()
                .extraArgs(List.of(givenRuleTypeIdNameDto()))
                .build();
    }

    public static IdNameDto givenRuleTypeIdNameDto() {
        return IdNameDto.builder()
                .id(1)
                .name(FIRST_EXTRA_ARG)
                .build();
    }
}
