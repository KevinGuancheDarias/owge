package com.kevinguanchedarias.owgejava.rest.open.wiki;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("/open/object-relations")
@ApplicationScope
@AllArgsConstructor
public class ObjectRelationRestService {
    private final ObjectRelationBo objectRelationBo;

    @GetMapping
    @Transactional
    public List<ObjectRelationDto> all() {
        return objectRelationBo.findAll()
                .stream()
                .map(entity -> DtoUtilService.staticDtoFromEntity(ObjectRelationDto.class, entity))
                .toList();
    }
}
