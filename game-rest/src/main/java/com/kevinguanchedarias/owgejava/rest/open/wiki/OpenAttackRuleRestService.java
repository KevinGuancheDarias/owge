package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.AttackRuleBo;
import com.kevinguanchedarias.owgejava.dto.AttackRuleDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/attack-rules")
@ApplicationScope
@AllArgsConstructor
public class OpenAttackRuleRestService {
    private final AttackRuleBo attackRuleBo;

    @GetMapping
    @Transactional
    public List<AttackRuleDto> all() {
        return attackRuleBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(AttackRuleDto.class, entity))
                .toList();
    }
}

