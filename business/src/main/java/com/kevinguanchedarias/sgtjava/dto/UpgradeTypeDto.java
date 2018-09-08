package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.sgtjava.entity.UpgradeType;

public class UpgradeTypeDto implements DtoFromEntity<UpgradeType> {

	private Integer id;
	private String name;

	@Override
	public void dtoFromEntity(UpgradeType entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
