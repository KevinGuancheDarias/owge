package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.GalaxyDto;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;

/**
 * 
 * @author Kevin Guanche Darias
 * @since 0.9.0
 *
 */
@RestController
@ApplicationScope
@RequestMapping("admin/galaxy")
public class AdminGalaxiesRestService implements CrudRestServiceTrait<Integer, Galaxy, GalaxyBo, GalaxyDto> {

	@Autowired
	private GalaxyBo galaxyBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@Override
	public RestCrudConfigBuilder<Integer, Galaxy, GalaxyBo, GalaxyDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, Galaxy, GalaxyBo, GalaxyDto> builder = RestCrudConfigBuilder.create();
		return builder.withBeanFactory(beanFactory).withBoService(galaxyBo).withEntityClass(Galaxy.class)
				.withDtoClass(GalaxyDto.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

	@GetMapping("/{id}/has-players")
	public boolean hasPlayers(@PathVariable Integer id) {
		return galaxyBo.hasPlayers(id);
	}

}
