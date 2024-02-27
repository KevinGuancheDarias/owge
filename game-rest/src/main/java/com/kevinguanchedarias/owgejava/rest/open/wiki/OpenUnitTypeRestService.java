package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.dto.wiki.UnitTypeWikiDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/unit-types")
@ApplicationScope
@AllArgsConstructor
public class OpenUnitTypeRestService {
    private final UnitTypeBo unitTypeBo;

    @GetMapping
    @Transactional
    public List<UnitTypeWikiDto> all() {
        return unitTypeBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(UnitTypeWikiDto.class, entity))
                .toList();
    }
}

