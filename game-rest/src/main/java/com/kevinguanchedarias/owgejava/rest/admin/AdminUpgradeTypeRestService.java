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
import com.kevinguanchedarias.owgejava.business.UpgradeTypeBo;
import com.kevinguanchedarias.owgejava.dto.UpgradeTypeDto;
import com.kevinguanchedarias.owgejava.entity.UpgradeType;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;

/**
 * 
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("admin/upgrade_type")
public class AdminUpgradeTypeRestService
		implements CrudRestServiceTrait<Integer, UpgradeType, UpgradeTypeBo, UpgradeTypeDto> {

	@Autowired
	private UpgradeTypeBo upgradeTypeBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait#
	 * getRestCrudConfigBuilder()
	 */
	@Override
	public RestCrudConfigBuilder<Integer, UpgradeType, UpgradeTypeBo, UpgradeTypeDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, UpgradeType, UpgradeTypeBo, UpgradeTypeDto> builder = RestCrudConfigBuilder
				.create();
		return builder.withBeanFactory(beanFactory).withBoService(upgradeTypeBo).withDtoClass(UpgradeTypeDto.class)
				.withEntityClass(UpgradeType.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

}
