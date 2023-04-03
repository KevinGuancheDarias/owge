package com.kevinguanchedarias.owgejava.rest.open;


import com.kevinguanchedarias.owgejava.annotation.WebControllerCache;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/websocket-sync")
@ApplicationScope
@AllArgsConstructor
public class OpenWebsocketSyncRestService {

    private final RuleRepository ruleRepository;
    private final ConversionService conversionService;

    @GetMapping("rule_change")
    @WebControllerCache(tags = Rule.RULE_CACHE_TAG)
    public List<RuleDto> findRules() {
        return ruleRepository.findAll().stream()
                .map(rule -> conversionService.convert(rule, RuleDto.class))
                .toList();
    }
}
