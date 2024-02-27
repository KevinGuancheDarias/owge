package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo;
import com.kevinguanchedarias.owgejava.dto.wiki.SpeedImpactGroupWikiDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/speed-impact-groups")
@ApplicationScope
@AllArgsConstructor
public class OpenSpeedImpactGroupRestService {
    private final SpeedImpactGroupBo speedImpactGroupBo;

    @GetMapping
    @Transactional
    public List<SpeedImpactGroupWikiDto> all() {
        return speedImpactGroupBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(SpeedImpactGroupWikiDto.class, entity))
                .toList();
    }
}

