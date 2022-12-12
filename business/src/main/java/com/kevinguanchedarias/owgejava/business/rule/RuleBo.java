package com.kevinguanchedarias.owgejava.business.rule;

import com.kevinguanchedarias.owgejava.business.rule.itemtype.RuleItemTypeProvider;
import com.kevinguanchedarias.owgejava.business.rule.type.RuleTypeProvider;
import com.kevinguanchedarias.owgejava.business.unit.util.UnitTypeInheritanceFinderService;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleItemTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class RuleBo {
    public static final String ARGS_DELIMITER = "#";

    private final RuleRepository ruleRepository;
    private final ConversionService conversionService;
    private final List<RuleItemTypeProvider> ruleItemTypeProviders;
    private final List<RuleTypeProvider> ruleTypeProviders;
    private final UnitTypeInheritanceFinderService unitTypeInheritanceFinderService;

    public List<RuleDto> findByOriginTypeAndOriginId(String originType, long id) {
        return ruleRepository.findByOriginTypeAndOriginId(originType, id).stream()
                .map(entity -> conversionService.convert(entity, RuleDto.class))
                .toList();
    }

    public List<RuleDto> findByOriginTypeAndOriginIdAndType(String originType, long id, String type) {
        return ruleRepository.findByOriginTypeAndOriginIdAndType(originType, id, type).stream()
                .map(entity -> conversionService.convert(entity, RuleDto.class))
                .toList();
    }

    public List<RuleDto> findByType(String type) {
        return ruleRepository.findByType(type)
                .stream()
                .map(entity -> conversionService.convert(entity, RuleDto.class))
                .toList();
    }

    public void deleteById(int id) {
        ruleRepository.deleteById(id);
    }

    public RuleDto save(RuleDto ruleDto) {
        var saved = ruleRepository.save(Objects.requireNonNull(conversionService.convert(ruleDto, Rule.class)));
        return conversionService.convert(saved, RuleDto.class);
    }

    public RuleItemTypeDescriptorDto findItemTypeDescriptor(String itemType) {
        return ruleItemTypeProviders.stream()
                .filter(ruleItemTypeProvider -> ruleItemTypeProvider.getRuleItemTypeId().equals(itemType))
                .findFirst()
                .map(RuleItemTypeProvider::findRuleItemTypeDescriptor)
                .orElseThrow(() -> new SgtBackendInvalidInputException("No item type " + itemType + " exists"));
    }

    public RuleTypeDescriptorDto findTypeDescriptor(String type) {
        return ruleTypeProviders.stream()
                .filter(ruleTypeProvider -> ruleTypeProvider.getRuleTypeId().equals(type))
                .findFirst()
                .map(RuleTypeProvider::findRuleTypeDescriptor)
                .orElseThrow(() -> new SgtBackendInvalidInputException("No type " + type + " exists"));
    }

    public Optional<String> findExtraArg(Rule rule, int position) {
        var splitted = rule.getExtraArgs().split(ARGS_DELIMITER);
        return splitted.length > position
                ? Optional.of(splitted[position])
                : Optional.empty();
    }

    public List<String> findExtraArgs(Rule rule) {
        return Arrays.asList(rule.getExtraArgs().split(ARGS_DELIMITER));
    }

    public boolean hasExtraArg(Rule rule, int position) {
        return findExtraArg(rule, position).isPresent();
    }

    public boolean isWantedType(RuleDto ruleDto, String type) {
        return ruleDto.getType().equals(type);
    }

    public boolean isWantedUnitDestination(RuleDto ruleDto, Unit unit) {
        if (ObjectEnum.UNIT.name().equals(ruleDto.getDestinationType())) {
            return unit.getId().equals(ruleDto.getDestinationId().intValue());
        } else if ("UNIT_TYPE".equals(ruleDto.getDestinationType())) {
            return unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(
                    unit.getType(),
                    unitType -> unitType.getId().equals(ruleDto.getDestinationId().intValue())
            ).isPresent();
        } else {
            log.debug("Unit {} is not wanted destination for rule {}", unit, ruleDto);
            return false;
        }
    }
}
