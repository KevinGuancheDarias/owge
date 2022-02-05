package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleItemTypeDescriptorDto;
import com.kevinguanchedarias.owgejava.dto.rule.RuleTypeDescriptorDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@ApplicationScope
@RequestMapping("admin/rules")
@AllArgsConstructor
public class AdminRuleRestService {
    private final RuleBo ruleBo;

    @GetMapping("/origin/{originType}/{id}")
    public List<RuleDto> findByOriginTypeAndOriginId(@PathVariable String originType, @PathVariable long id) {
        return ruleBo.findByOriginTypeAndOriginId(originType, id);
    }

    @GetMapping("/type/{type}")
    public List<RuleDto> findByType(@PathVariable String type) {
        return ruleBo.findByType(type);
    }

    @GetMapping("/type-descriptor/{type}")
    public RuleTypeDescriptorDto findRuleTypeDescriptor(@PathVariable String type) {
        return ruleBo.findTypeDescriptor(type);
    }

    @GetMapping("/item-type-descriptor/{itemType}")
    public RuleItemTypeDescriptorDto findRuleItemTypeDescriptor(@PathVariable String itemType) {
        return ruleBo.findItemTypeDescriptor(itemType);
    }

    @DeleteMapping("{id}")
    public void deleteById(@PathVariable int id) {
        ruleBo.deleteById(id);
    }

    @PostMapping
    public RuleDto save(@RequestBody RuleDto ruleDto) {
        return ruleBo.save(ruleDto);
    }
}
