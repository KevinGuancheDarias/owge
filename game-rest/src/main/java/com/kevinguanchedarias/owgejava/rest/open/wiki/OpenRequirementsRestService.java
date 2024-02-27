package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.dto.RequirementDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/requirements")
@ApplicationScope
@AllArgsConstructor
public class OpenRequirementsRestService {
    private final RequirementBo requirementBo;

    @GetMapping
    @Transactional
    public List<RequirementDto> all() {
        return requirementBo.findAll()
                .stream()
                .map(requirement -> DtoUtilService.staticDtoFromEntity(RequirementDto.class, requirement))
                .toList();
    }
}
