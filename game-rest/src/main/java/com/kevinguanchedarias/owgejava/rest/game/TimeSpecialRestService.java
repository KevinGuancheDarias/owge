/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.game;

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
import com.kevinguanchedarias.owgejava.rest.trait.WithUnlockedRestServiceTrait;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("game/time_special")
public class TimeSpecialRestService
		implements WithUnlockedRestServiceTrait<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto> {

	@Autowired
	private TimeSpecialBo timeSpecialBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

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
	 * @see com.kevinguanchedarias.owgejava.rest.trait.WithUnlockedRestServiceTrait#
	 * getObject()
	 */
	@Override
	public ObjectEnum getObject() {
		return ObjectEnum.TIME_SPECIAL;
	}
}
