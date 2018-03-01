package com.kevinguanchedarias.sgtjava.pojo;

import java.util.List;

import com.kevinguanchedarias.sgtjava.dto.GalaxyDto;
import com.kevinguanchedarias.sgtjava.dto.PlanetDto;

public class NavigationPojo {
	private List<GalaxyDto> galaxies;
	private List<PlanetDto> planets;

	public List<GalaxyDto> getGalaxies() {
		return galaxies;
	}

	public void setGalaxies(List<GalaxyDto> galaxies) {
		this.galaxies = galaxies;
	}

	/**
	 * All the planets in the selected sector and quadrant
	 *
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<PlanetDto> getPlanets() {
		return planets;
	}

	public void setPlanets(List<PlanetDto> planets) {
		this.planets = planets;
	}
}
