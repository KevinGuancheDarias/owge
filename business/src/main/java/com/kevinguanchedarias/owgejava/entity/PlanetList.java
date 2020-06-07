package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;

@Entity
@Table(name = "planet_list")
public class PlanetList {

	@EmbeddedId
	private PlanetUser planetUser;

	@Column(length = 150)
	private String name;

	/**
	 * @return the planetUser
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public PlanetUser getPlanetUser() {
		return planetUser;
	}

	/**
	 * @param planetUser the planetUser to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setPlanetUser(PlanetUser planetUser) {
		this.planetUser = planetUser;
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
