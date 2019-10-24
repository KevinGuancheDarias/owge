/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithImprovementsRestServiceTrait;

/**
 * 
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@Scope()
@RequestMapping("admin/faction")
public class AdminFactionRestService
		implements CrudWithImprovementsRestServiceTrait<Integer, Faction, FactionBo, FactionDto> {

	@Autowired
	private FactionBo factionBo;
	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * CrudWithImprovementsRestServiceTrait#getRestCrudConfigBuilder()
	 */
	@Override
	public RestCrudConfigBuilder<Integer, Faction, FactionBo, FactionDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, Faction, FactionBo, FactionDto> builder = RestCrudConfigBuilder.create();
		return builder.withBeanFactory(beanFactory).withBoService(factionBo).withDtoClass(FactionDto.class)
				.withEntityClass(Faction.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

}
