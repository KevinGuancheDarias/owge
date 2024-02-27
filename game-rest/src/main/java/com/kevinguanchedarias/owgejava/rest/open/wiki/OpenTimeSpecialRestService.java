package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.TimeSpecialBo;
import com.kevinguanchedarias.owgejava.dto.wiki.TimeSpecialWikiDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/time-specials")
@ApplicationScope
@AllArgsConstructor
public class OpenTimeSpecialRestService {
    private final TimeSpecialBo timeSpecialBo;

    @GetMapping
    @Transactional
    public List<TimeSpecialWikiDto> all() {
        return timeSpecialBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(TimeSpecialWikiDto.class, entity))
                .toList();
    }
}

