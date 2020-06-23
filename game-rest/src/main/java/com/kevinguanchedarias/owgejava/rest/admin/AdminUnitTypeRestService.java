/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo;
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

	@Autowired
	private SpeedImpactGroupBo speedImpactGroupBo;

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public Optional<UnitType> beforeSave(UnitTypeDto parsedDto, UnitType entity) {
		if (parsedDto.getImage() != null) {
			entity.setImage(getRestCrudConfigBuilder().build().getBeanFactory().getBean(ImageStoreBo.class)
					.findByIdOrDie(parsedDto.getImage()));
		}
		if (parsedDto.getSpeedImpactGroup() != null && parsedDto.getSpeedImpactGroup().getId() != null) {
			entity.setSpeedImpactGroup(speedImpactGroupBo.findByIdOrDie(parsedDto.getSpeedImpactGroup().getId()));
		}
		return CrudRestServiceTrait.super.beforeSave(parsedDto, entity);
	}

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
