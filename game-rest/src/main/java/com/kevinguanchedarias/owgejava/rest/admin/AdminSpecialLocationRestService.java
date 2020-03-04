/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SpecialLocationBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.SpecialLocationDto;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithImprovementsRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;

/**
 * 
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("admin/special-location")
public class AdminSpecialLocationRestService implements
		CrudWithImprovementsRestServiceTrait<Integer, SpecialLocation, SpecialLocationBo, SpecialLocationDto>,
		WithImageRestServiceTrait<Integer, SpecialLocation, SpecialLocationDto, SpecialLocationBo> {

	@Autowired
	private SpecialLocationBo specialLocationBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * CrudWithRequirementsRestServiceTrait#getRestCrudConfigBuilder()
	 */
	@Override
	public RestCrudConfigBuilder<Integer, SpecialLocation, SpecialLocationBo, SpecialLocationDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, SpecialLocation, SpecialLocationBo, SpecialLocationDto> builder = RestCrudConfigBuilder
				.create();
		return builder.withBeanFactory(beanFactory).withBoService(specialLocationBo)
				.withDtoClass(SpecialLocationDto.class).withEntityClass(SpecialLocation.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

	@Override
	public Optional<SpecialLocation> beforeSave(SpecialLocationDto parsedDto, SpecialLocation entity) {
		Galaxy galaxy = new Galaxy();
		galaxy.setId(parsedDto.getGalaxyId());
		entity.setGalaxy(galaxy);
		CrudWithImprovementsRestServiceTrait.super.beforeSave(parsedDto, entity);
		return WithImageRestServiceTrait.super.beforeSave(parsedDto, entity);
	}

}
