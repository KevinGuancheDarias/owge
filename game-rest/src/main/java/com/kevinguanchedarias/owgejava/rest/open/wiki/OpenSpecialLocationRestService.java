package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.SpecialLocationBo;
import com.kevinguanchedarias.owgejava.dto.wiki.SpecialLocationWikiDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/special-locations")
@ApplicationScope
@AllArgsConstructor
public class OpenSpecialLocationRestService {
    private final SpecialLocationBo specialLocationBo;

    @GetMapping
    @Transactional
    public List<SpecialLocationWikiDto> all() {
        return specialLocationBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(SpecialLocationWikiDto.class, entity))
                .toList();
    }
}

