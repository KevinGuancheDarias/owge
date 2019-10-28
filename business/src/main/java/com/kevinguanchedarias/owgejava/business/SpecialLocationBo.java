package com.kevinguanchedarias.owgejava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.repository.SpecialLocationRepository;

@Service
public class SpecialLocationBo implements WithNameBo<Integer, SpecialLocation, DtoFromEntity<SpecialLocation>> {
	private static final long serialVersionUID = 2511602524693638404L;

	@Autowired
	private SpecialLocationRepository specialLocationRepository;

	@Autowired
	private PlanetBo planetBo;

	@Override
	public JpaRepository<SpecialLocation, Integer> getRepository() {
		return specialLocationRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<DtoFromEntity<SpecialLocation>> getDtoClass() {
		throw new SgtBackendNotImplementedException("SpecialLocation doesn't have a dto ... for now =/");
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
