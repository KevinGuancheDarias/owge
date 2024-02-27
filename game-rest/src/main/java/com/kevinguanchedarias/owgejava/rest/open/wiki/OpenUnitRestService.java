package com.kevinguanchedarias.owgejava.rest.open.wiki;


import com.kevinguanchedarias.owgejava.business.UnitBo;
import com.kevinguanchedarias.owgejava.dto.wiki.UnitWikiDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/units")
@ApplicationScope
@AllArgsConstructor
public class OpenUnitRestService {
    private final UnitBo unitBo;

    @GetMapping
    @Transactional
    public List<UnitWikiDto> all() {
        return unitBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(UnitWikiDto.class, entity))
                .toList();
    }
}

