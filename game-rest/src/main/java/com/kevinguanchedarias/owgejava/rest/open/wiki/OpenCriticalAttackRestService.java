package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.CriticalAttackBo;
import com.kevinguanchedarias.owgejava.dto.CriticalAttackDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/critical-attacks")
@ApplicationScope
@AllArgsConstructor
public class OpenCriticalAttackRestService {
    private final CriticalAttackBo criticalAttackBo;

    @GetMapping
    @Transactional
    public List<CriticalAttackDto> all() {
        return criticalAttackBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(CriticalAttackDto.class, entity))
                .toList();
    }
}

