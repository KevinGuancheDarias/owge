package com.kevinguanchedarias.owgejava.entity.embeddedid;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

@Embeddable
public class PlanetUser implements Serializable {
	private static final long serialVersionUID = -6385505776318319556L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = true)
	@Fetch(FetchMode.JOIN)
	private UserStorage user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "planet_id", nullable = true)
	@Fetch(FetchMode.JOIN)
	private Planet planet;

	public PlanetUser() {

	}

	/**
	 *
	 * @param user
	 * @param planet
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public PlanetUser(UserStorage user, Planet planet) {
		super();
		this.user = user;
		this.planet = planet;
	}

	/**
	 * @return the user
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UserStorage getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setUser(UserStorage user) {
		this.user = user;
	}

	/**
	 * @return the planet
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Planet getPlanet() {
		return planet;
	}

	/**
	 * @param planet the planet to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setPlanet(Planet planet) {
		this.planet = planet;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((planet == null) ? 0 : planet.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PlanetUser other = (PlanetUser) obj;
		if (planet == null) {
			if (other.planet != null) {
				return false;
			}
		} else if (!planet.equals(other.planet)) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		} else if (!user.equals(other.user)) {
			return false;
		}
		return true;
	}

}
