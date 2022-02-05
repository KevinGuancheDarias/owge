package com.kevinguanchedarias.owgejava.converter.rule;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.Rule;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RuleDtoToEntityConverter implements Converter<RuleDto, Rule> {
    @Override
    public Rule convert(RuleDto source) {
        return Rule.builder()
                .id(source.getId())
                .type(source.getType())
                .originType(source.getOriginType())
                .originId(source.getOriginId())
                .destinationType(source.getDestinationType())
                .destinationId(source.getDestinationId())
                .extraArgs(convertExtraArgs(source.getExtraArgs()))
                .build();
    }

    private String convertExtraArgs(List<Object> extraArgs) {
        return extraArgs.stream()
                .map(Object::toString)
                .filter(string -> !string.contains(RuleBo.ARGS_DELIMITER))
                .collect(Collectors.joining(RuleBo.ARGS_DELIMITER));
    }
}
