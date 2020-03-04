package com.kevinguanchedarias.owgejava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.SpecialLocationDto;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.repository.SpecialLocationRepository;

@Service
public class SpecialLocationBo implements WithNameBo<Integer, SpecialLocation, SpecialLocationDto> {
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
	public Class<SpecialLocationDto> getDtoClass() {
		return SpecialLocationDto.class;
	}

	@Override
	@Transactional
	public SpecialLocation save(SpecialLocation entity) {
		Planet assignedPlanet = null;
		if (entity.getId() != null) {
			SpecialLocation stored = findByIdOrDie(entity.getId());
			if (stored.getGalaxy() == null || !stored.getGalaxy().equals(entity.getGalaxy())) {
				assignedPlanet = assignPlanet(entity);
				if (stored.getAssignedPlanet() != null) {
					stored.getAssignedPlanet().setSpecialLocation(null);
					planetBo.save(stored.getAssignedPlanet());
				}
			}
		}
		if (assignedPlanet != null) {
			entity.setGalaxy(assignedPlanet.getGalaxy());
		} else {
			entity.setGalaxy(null);
		}
		SpecialLocation saved = WithNameBo.super.save(entity);
		if (assignedPlanet != null) {
			assignedPlanet.setSpecialLocation(saved);
			planetBo.save(assignedPlanet);
		}
		saved.setAssignedPlanet(assignedPlanet);
		return saved;
	}

	@Override
	@Transactional
	public void delete(SpecialLocation entity) {
		if (entity.getAssignedPlanet() != null) {
			Planet planet = planetBo.findById(entity.getAssignedPlanet().getId());
			if (planet != null) {
				planet.setSpecialLocation(null);
				planetBo.save(planet);
			}
		}
		WithNameBo.super.delete(entity);
	}

	/**
	 * Will return a valid planet for the special location
	 * 
	 * @param specialLocation If the galaxy is null will not be assigned, if 0 will
	 *                        choose a random galaxy
	 */
	private Planet assignPlanet(SpecialLocation specialLocation) {
		Integer galaxyId = specialLocation.getGalaxy() != null ? specialLocation.getGalaxy().getId() : null;
		if (galaxyId != null) {
			return planetBo.findRandomPlanet(galaxyId.equals(0) ? null : galaxyId);
		} else {
			return null;
		}
	}

}
