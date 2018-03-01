package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.sgtjava.entity.Galaxy;

public class GalaxyDto implements DtoFromEntity<Galaxy> {

	private Integer id;
	private String name;
	private Long sectors;
	private Long quadrants;

	@Override
	public void dtoFromEntity(Galaxy entity) {
		id = entity.getId();
		name = entity.getName();
		sectors = entity.getSectors();
		quadrants = entity.getQuadrants();
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

	public Long getSectors() {
		return sectors;
	}

	public void setSectors(Long sectors) {
		this.sectors = sectors;
	}

	public Long getQuadrants() {
		return quadrants;
	}

	public void setQuadrants(Long quadrants) {
		this.quadrants = quadrants;
	}

}
