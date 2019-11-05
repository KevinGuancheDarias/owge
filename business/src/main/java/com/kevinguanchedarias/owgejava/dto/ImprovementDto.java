package com.kevinguanchedarias.owgejava.dto;

import java.util.ArrayList;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.UserImprovement;

public class ImprovementDto extends AbstractImprovementDto implements DtoFromEntity<Improvement> {
	private Integer id;

	@Override
	public void dtoFromEntity(Improvement entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
		setUnitTypesUpgrades(new ArrayList<>());
		if (entity.getUnitTypesUpgrades() != null) {
			entity.getUnitTypesUpgrades().forEach(current -> {
				ImprovementUnitTypeDto currentDto = new ImprovementUnitTypeDto();
				currentDto.dtoFromEntity(current);
				getUnitTypesUpgrades().add(currentDto);
			});
		}
	}

	public void dtoFromEntity(UserImprovement entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
