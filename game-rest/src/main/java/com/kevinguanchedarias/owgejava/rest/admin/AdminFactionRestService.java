/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.FactionUnitTypeDto;
import com.kevinguanchedarias.owgejava.pojo.UnitTypesOverride;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithImprovementsRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;

/**
 *
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("admin/faction")
public class AdminFactionRestService
		implements CrudWithImprovementsRestServiceTrait<Integer, Faction, FactionBo, FactionDto>,
		WithImageRestServiceTrait<Integer, Faction, FactionDto, FactionBo> {

	@Autowired
	private FactionBo factionBo;

	@Autowired
	private ImageStoreBo imageStoreBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	/**
	 *
	 * @param factionId
	 * @return
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("{factionId}/unitTypes")
	public List<FactionUnitTypeDto> findUnitTypesOverrides(@PathVariable Integer factionId) {
		Faction faction = factionBo.findByIdOrDie(factionId);
		return faction.getUnitTypes().stream().map(current -> {
			FactionUnitTypeDto dto = new FactionUnitTypeDto();
			dto.dtoFromEntity(current);
			dto.setFactionId(null);
			return dto;
		}).collect(Collectors.toList());
	}

	/**
	 *
	 * @param factionId
	 * @param overrides
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PutMapping("{factionId}/unitTypes")
	public void saveUnitTypes(@PathVariable Integer factionId, @RequestBody List<UnitTypesOverride> overrides) {
		factionBo.saveOverrides(factionId, overrides);
	}

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

	@Override
	public Optional<Faction> beforeSave(FactionDto parsedDto, Faction entity) {
		Long primaryResourceImage = parsedDto.getPrimaryResourceImage();
		Long secondaryResourceImage = parsedDto.getSecondaryResourceImage();
		Long energyResourceImage = parsedDto.getEnergyImage();
		if (primaryResourceImage != null) {
			entity.setPrimaryResourceImage(imageStoreBo.findByIdOrDie(primaryResourceImage));
		}
		if (secondaryResourceImage != null) {
			entity.setSecondaryResourceImage(imageStoreBo.findByIdOrDie(secondaryResourceImage));
		}
		if (energyResourceImage != null) {
			entity.setEnergyImage(imageStoreBo.findByIdOrDie(energyResourceImage));
		}
		return WithImageRestServiceTrait.super.beforeSave(parsedDto, entity);
	}

}
