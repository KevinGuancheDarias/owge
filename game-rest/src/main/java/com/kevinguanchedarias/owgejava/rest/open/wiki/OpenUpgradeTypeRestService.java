package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.UpgradeTypeBo;
import com.kevinguanchedarias.owgejava.dto.UpgradeTypeDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/open/upgrade-types")
@ApplicationScope
@AllArgsConstructor
public class OpenUpgradeTypeRestService {
    private final UpgradeTypeBo upgradeTypeBo;

    @GetMapping
    @Transactional
    public List<UpgradeTypeDto> all() {
        return upgradeTypeBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(UpgradeTypeDto.class, entity))
                .sorted(Comparator.comparing(UpgradeTypeDto::getId))
                .toList();
    }
}

