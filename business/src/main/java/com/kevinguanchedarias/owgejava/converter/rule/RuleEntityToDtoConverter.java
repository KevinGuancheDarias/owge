package com.kevinguanchedarias.owgejava.converter.rule;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.Rule;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RuleEntityToDtoConverter implements Converter<Rule, RuleDto> {
    @Override
    public RuleDto convert(Rule source) {
        return RuleDto.builder()
                .id(source.getId())
                .type(source.getType())
                .originType(source.getOriginType())
                .originId(source.getOriginId())
                .destinationType(source.getDestinationType())
                .destinationId(source.getDestinationId())
                .extraArgs(Arrays.asList(source.getExtraArgs().split(RuleBo.ARGS_DELIMITER)))
                .build();
    }
}
