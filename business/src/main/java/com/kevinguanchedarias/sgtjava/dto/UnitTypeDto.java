package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.sgtjava.entity.UnitType;

public class UnitTypeDto implements DtoFromEntity<UnitType> {

	private Integer id;
	private String name;
	private Long maxCount;
	private Long computedMaxCount;
	private Long userBuilt;

	@Override
	public void dtoFromEntity(UnitType entity) {
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

	public Long getMaxCount() {
		return maxCount;
	}

	public void setMaxCount(Long maxCount) {
		this.maxCount = maxCount;
	}

	/**
	 * Transient property, when defined, represents the maxCount with all user
	 * improvements applied
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getComputedMaxCount() {
		return computedMaxCount;
	}

	public void setComputedMaxCount(Long computedMaxCount) {
		this.computedMaxCount = computedMaxCount;
	}

	/**
	 * Represents the amount built by the user
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getUserBuilt() {
		return userBuilt;
	}

	public void setUserBuilt(Long userBuilt) {
		this.userBuilt = userBuilt;
	}

}
