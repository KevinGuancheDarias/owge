package com.kevinguanchedarias.owgejava.dto.rule;


import com.kevinguanchedarias.owgejava.dto.CommonDto;
import com.kevinguanchedarias.owgejava.entity.Unit;

import java.util.List;
import java.util.Map;

public record RuleWithRelatedUnitsDto(List<RuleDto> rules, Map<Integer, CommonDto<Integer, Unit>> relatedUnits) {
}
