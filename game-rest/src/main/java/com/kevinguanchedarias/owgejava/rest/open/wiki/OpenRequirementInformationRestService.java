package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.RequirementInformationBo;
import com.kevinguanchedarias.owgejava.dto.wiki.RequirementInformationWikiDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/requirement-information")
@ApplicationScope
@AllArgsConstructor
public class OpenRequirementInformationRestService {
    private final RequirementInformationBo requirementInformationBo;

    @GetMapping
    @Transactional
    public List<RequirementInformationWikiDto> all() {
        return requirementInformationBo.findAll().stream()
                .map(requirementInformation -> DtoUtilService.staticDtoFromEntity(RequirementInformationWikiDto.class, requirementInformation))
                .toList();
    }
}
