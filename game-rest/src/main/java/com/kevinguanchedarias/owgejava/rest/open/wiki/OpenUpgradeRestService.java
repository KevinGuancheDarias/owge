package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.UpgradeBo;
import com.kevinguanchedarias.owgejava.dto.wiki.UpgradeWikiDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/upgrades")
@ApplicationScope
@AllArgsConstructor
public class OpenUpgradeRestService {
    private final UpgradeBo upgradeBo;

    @GetMapping
    @Transactional
    public List<UpgradeWikiDto> all() {
        return upgradeBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(UpgradeWikiDto.class, entity))
                .toList();
    }
}

