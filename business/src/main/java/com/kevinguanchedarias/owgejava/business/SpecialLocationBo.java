package com.kevinguanchedarias.owgejava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.repository.SpecialLocationRepository;

@Service
public class SpecialLocationBo implements WithNameBo<SpecialLocation> {
	private static final long serialVersionUID = 2511602524693638404L;

	@Autowired
	private SpecialLocationRepository specialLocationRepository;

	@Autowired
	private PlanetBo planetBo;

	@Override
	public JpaRepository<SpecialLocation, Number> getRepository() {
		return specialLocationRepository;
	}

	/**
	 * Will return a valid planet for the special location
	 * 
	 * @param specialLocation
	 */
	public Planet assignPlanet(SpecialLocation specialLocation) {
		return planetBo.findRandomPlanet(specialLocation.getGalaxy().getId());
	}
}
