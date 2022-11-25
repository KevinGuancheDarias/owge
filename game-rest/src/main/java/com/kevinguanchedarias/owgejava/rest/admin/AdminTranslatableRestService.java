package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.TranslationBo;
import com.kevinguanchedarias.owgejava.dto.TranslatableDto;
import com.kevinguanchedarias.owgejava.dto.TranslatableTranslationDto;
import com.kevinguanchedarias.owgejava.entity.Translatable;
import com.kevinguanchedarias.owgejava.repository.TranslatableRepository;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithDeleteRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithReadRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/translatable")
@AllArgsConstructor
public class AdminTranslatableRestService
        implements WithReadRestServiceTrait<Long, Translatable, TranslatableRepository, TranslatableDto>,
        CrudRestServiceTrait<Long, Translatable, TranslatableRepository, TranslatableDto>,
        WithDeleteRestServiceTrait<Long, Translatable, TranslatableRepository, TranslatableDto> {

    private final TranslatableRepository translatableRepository;

    private final AutowireCapableBeanFactory beanFactory;
    private final TranslationBo translationBo;

    @Override
    public RestCrudConfigBuilder<Long, Translatable, TranslatableRepository, TranslatableDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Long, Translatable, TranslatableRepository, TranslatableDto> builder = RestCrudConfigBuilder
                .create();
        return builder.withBeanFactory(beanFactory).withRepository(translatableRepository).withDtoClass(TranslatableDto.class)
                .withEntityClass(Translatable.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @GetMapping("{id}/translations")
    public List<TranslatableTranslationDto> findTranslations(@PathVariable Long id) {
        return translationBo.findTranslations(id);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostMapping("{id}/translations")
    @PutMapping("{id}/translations")
    public TranslatableTranslationDto addTranslation(@PathVariable Long id,
                                                     @RequestBody TranslatableTranslationDto translationDto) {
        return translationBo.addTranslation(translationBo.toDto(SpringRepositoryUtil.findByIdOrDie(translatableRepository, id)), translationDto);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @DeleteMapping("{id}/translations/{translationId}")
    public void deleteTranslation(@PathVariable Long translationId) {
        translatableRepository.deleteById(translationId);
    }
}
