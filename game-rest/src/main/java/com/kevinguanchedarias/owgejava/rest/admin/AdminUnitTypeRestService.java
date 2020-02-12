/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;

/**
 * 
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("admin/unit_type")
public class AdminUnitTypeRestService implements CrudRestServiceTrait<Integer, UnitType, UnitTypeBo, UnitTypeDto> {

	@Autowired
	private UnitTypeBo unitTypeBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait#
	 * getRestCrudConfigBuilder()
	 */
	@Override
	public RestCrudConfigBuilder<Integer, UnitType, UnitTypeBo, UnitTypeDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, UnitType, UnitTypeBo, UnitTypeDto> builder = RestCrudConfigBuilder.create();
		return builder.withBeanFactory(beanFactory).withBoService(unitTypeBo).withDtoClass(UnitTypeDto.class)
				.withEntityClass(UnitType.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

}
