package com.kevinguanchedarias.owgejava.rest.open;


import com.kevinguanchedarias.owgejava.annotation.WebControllerCache;
import com.kevinguanchedarias.owgejava.business.CommonEntityService;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.dto.CommonDto;
import com.kevinguanchedarias.owgejava.dto.SpeedImpactGroupDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleWithRelatedUnitsDto;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/open/websocket-sync")
@ApplicationScope
@AllArgsConstructor
public class OpenWebsocketSyncRestService {

    private final RuleRepository ruleRepository;
    private final ConversionService conversionService;
    private final CommonEntityService commonEntityService;
    private final UnitRepository unitRepository;
    private final SpeedImpactGroupRepository speedImpactGroupRepository;

    @GetMapping("rule_change")
    @WebControllerCache(tags = {
            Rule.RULE_CACHE_TAG,
            Unit.UNIT_CACHE_TAG
    })
    public RuleWithRelatedUnitsDto findRules() {
        var rules = ruleRepository.findAll().stream()
                .map(rule -> conversionService.convert(rule, RuleDto.class))
                .toList();
        Map<Integer, CommonDto<Integer, Unit>> units = commonEntityService.entitiesByIdToDto(
                unitRepository,
                Stream.concat(
                        rules.stream()
                                .filter(rule -> RuleBo.UNIT_RULE.equals(rule.getOriginType()))
                                .map(rule -> rule.getOriginId().intValue()),
                        rules.stream()
                                .filter(rule -> RuleBo.UNIT_RULE.equals(rule.getDestinationType()))
                                .map(rule -> rule.getDestinationId().intValue())
                ).collect(Collectors.toUnmodifiableSet())

        );
        return new RuleWithRelatedUnitsDto(rules, units);
    }

    @GetMapping("speed_group_change")
    @WebControllerCache(tags = SpeedImpactGroup.SPEED_IMPACT_GROUP_CACHE_TAG)
    public List<SpeedImpactGroupDto> findSpeedImpactGroups() {
        return speedImpactGroupRepository.findAll().stream()
                .map(speedImpactGroup -> {
                    var dto = new SpeedImpactGroupDto();
                    dto.dtoFromEntity(speedImpactGroup);
                    return dto;
                })
                .toList();
    }
}
