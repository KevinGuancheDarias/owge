package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.PlanetList;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class PlanetListDto implements DtoFromEntity<PlanetList> {
	private Integer userId;
	private String username;
	private PlanetDto planet;
	private String name;

	@Override
	public void dtoFromEntity(PlanetList entity) {
		UserStorage user = entity.getPlanetUser().getUser();
		userId = user.getId();
		username = user.getUsername();
		planet = new PlanetDto();
		planet.dtoFromEntity(entity.getPlanetUser().getPlanet());
		name = entity.getName();
	}

	/**
	 * @return the userId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	/**
	 * @return the username
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the planet
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public PlanetDto getPlanet() {
		return planet;
	}

	/**
	 * @param planet the planet to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setPlanet(PlanetDto planet) {
		this.planet = planet;
	}

	/**
	 * @return the name
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setName(String name) {
		this.name = name;
	}

}
