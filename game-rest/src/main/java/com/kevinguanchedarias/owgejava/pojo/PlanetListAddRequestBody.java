package com.kevinguanchedarias.owgejava.pojo;

/**
 * Represents the required properties to add a PlanetList from a rest endpoint
 * 
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class PlanetListAddRequestBody {

	private Long planetId;
	private String name;

	/**
	 * @return the planetId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getPlanetId() {
		return planetId;
	}

	/**
	 * @param planetId the planetId to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setPlanetId(Long planetId) {
		this.planetId = planetId;
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
