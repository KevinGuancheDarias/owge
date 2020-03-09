package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.TimeSpecialBo;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithFullRestService;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("admin/time_special")
public class AdminTimeSpecialRestService
		implements CrudWithFullRestService<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto>,
		WithImageRestServiceTrait<Integer, TimeSpecial, TimeSpecialDto, TimeSpecialBo> {

	@Autowired
	private TimeSpecialBo timeSpecialBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * CrudWithRequirementsRestServiceTrait#getRestCrudConfigBuilder()
	 */
	@Override
	public RestCrudConfigBuilder<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto> builder = RestCrudConfigBuilder
				.create();
		return builder.withBeanFactory(beanFactory).withBoService(timeSpecialBo).withDtoClass(TimeSpecialDto.class)
				.withEntityClass(TimeSpecial.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * CrudWithRequirementsRestServiceTrait#getObject()
	 */
	@Override
	public ObjectEnum getObject() {
		return ObjectEnum.TIME_SPECIAL;
	}

	@Override
	public Optional<TimeSpecial> beforeSave(TimeSpecialDto parsedDto, TimeSpecial entity) {
		CrudWithFullRestService.super.beforeSave(parsedDto, entity);
		return WithImageRestServiceTrait.super.beforeSave(parsedDto, entity);
	}

}
