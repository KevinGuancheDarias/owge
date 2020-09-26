package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.TranslationBo;
import com.kevinguanchedarias.owgejava.dto.TranslatableDto;
import com.kevinguanchedarias.owgejava.dto.TranslatableTranslationDto;
import com.kevinguanchedarias.owgejava.entity.Translatable;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithDeleteRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithReadRestServiceTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@ApplicationScope
@RequestMapping("admin/translatable")
public class AdminTranslatableRestService
		implements WithReadRestServiceTrait<Long, Translatable, TranslationBo, TranslatableDto>,
		CrudRestServiceTrait<Long, Translatable, TranslationBo, TranslatableDto>,
		WithDeleteRestServiceTrait<Long, Translatable, TranslationBo, TranslatableDto> {

	@Autowired
	private TranslationBo translationBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@Override
	public RestCrudConfigBuilder<Long, Translatable, TranslationBo, TranslatableDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Long, Translatable, TranslationBo, TranslatableDto> builder = RestCrudConfigBuilder
				.create();
		return builder.withBeanFactory(beanFactory).withBoService(translationBo).withDtoClass(TranslatableDto.class)
				.withEntityClass(Translatable.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

	/**
	 *
	 * @param id
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("{id}/translations")
	public List<TranslatableTranslationDto> findTranslations(@PathVariable Long id) {
		return translationBo.findTranslations(id);
	}

	/**
	 *
	 * @param id
	 * @param translationDto
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("{id}/translations")
	@PutMapping("{id}/translations")
	public TranslatableTranslationDto addTranslation(@PathVariable Long id,
			@RequestBody TranslatableTranslationDto translationDto) {
		return translationBo.addTranslation(translationBo.toDto(translationBo.findByIdOrDie(id)), translationDto);
	}

	/**
	 *
	 * @param translationId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@DeleteMapping("{id}/translations/{translationId}")
	public void deleteTranslation(@PathVariable Long translationId) {
		translationBo.deleteTranslation(translationId);
	}
}
