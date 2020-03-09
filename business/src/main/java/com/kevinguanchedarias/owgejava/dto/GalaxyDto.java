package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Galaxy;

public class GalaxyDto implements DtoFromEntity<Galaxy> {

	private Integer id;
	private String name;
	private Long sectors;
	private Long quadrants;
	private Integer orderNumber;

	@Override
	public void dtoFromEntity(Galaxy entity) {
		id = entity.getId();
		name = entity.getName();
		sectors = entity.getSectors();
		quadrants = entity.getQuadrants();
		orderNumber = entity.getOrderNumber();
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

	/**
	 * 
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @return
	 */
	public Integer getOrderNumber() {
		return orderNumber;
	}

	/**
	 * @param orderNumber the orderNumber to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setOrderNumber(Integer orderNumber) {
		this.orderNumber = orderNumber;
	}

}
