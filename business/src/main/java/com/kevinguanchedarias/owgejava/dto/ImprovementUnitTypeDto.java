package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;

public class ImprovementUnitTypeDto implements DtoFromEntity<ImprovementUnitType> {
	private Integer id;
	private String type;
	private Integer unitTypeId;
	private String unitTypeName;
	private Long value;

	@Override
	public void dtoFromEntity(ImprovementUnitType entity) {
		id = entity.getId();
		type = entity.getType();
		unitTypeId = entity.getUnitType().getId();
		unitTypeName = entity.getUnitType().getName();
		value = entity.getValue();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getUnitTypeId() {
		return unitTypeId;
	}

	public void setUnitTypeId(Integer unitTypeId) {
		this.unitTypeId = unitTypeId;
	}

	public String getUnitTypeName() {
		return unitTypeName;
	}

	public void setUnitTypeName(String unitTypeName) {
		this.unitTypeName = unitTypeName;
	}

	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
	}

}
